#!/bin/bash
# ==========================================
# Langtou - Elasticsearch 快照备份脚本
# 功能: 创建 Elasticsearch 索引快照备份，保留7天
# 用法: ./es-backup.sh [--verify] [--cleanup-only] [--dry-run]
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
ES_HOST="${ES_HOST:-http://127.0.0.1:9200}"
ES_USER="${ES_USER:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-}"
ES_INDEX_PATTERNS="${ES_INDEX_PATTERNS:-langtou_*}"  # 要备份的索引模式

BACKUP_DIR="/data/backups/elasticsearch"
REPO_NAME="langtou_backup_repo"
SNAPSHOT_PREFIX="langtou_snap"
RETENTION_DAYS=7
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
SNAPSHOT_NAME="${SNAPSHOT_PREFIX}_${BACKUP_DATE}"
LOG_FILE="/var/log/langtou/es-backup.log"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ==========================================
# 工具函数
# ==========================================
log_info() {
    echo -e "${GREEN}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "${LOG_FILE}"
}

# ES HTTP 请求封装
es_curl() {
    local method="$1"
    local path="$2"
    local data="${3:-}"

    local url="${ES_HOST}${path}"
    local auth_header=""
    if [[ -n "${ES_PASSWORD}" ]]; then
        auth_header="-u ${ES_USER}:${ES_PASSWORD}"
    fi

    if [[ -n "${data}" ]]; then
        curl -s -X "${method}" "${url}" ${auth_header} \
            -H "Content-Type: application/json" \
            -d "${data}"
    else
        curl -s -X "${method}" "${url}" ${auth_header} \
            -H "Content-Type: application/json"
    fi
}

# ==========================================
# 前置检查
# ==========================================
check_prerequisites() {
    log_step "检查前置条件..."

    # 检查 curl
    if ! command -v curl &>/dev/null; then
        log_error "curl 未安装"
        exit 1
    fi

    # 检查 ES 连接
    local health
    health=$(es_curl "GET" "/_cluster/health" 2>/dev/null) || {
        log_error "无法连接到 Elasticsearch: ${ES_HOST}"
        exit 1
    }

    local status
    status=$(echo "${health}" | jq -r '.status' 2>/dev/null)
    if [[ "${status}" == "red" ]]; then
        log_error "Elasticsearch 集群状态为 RED，无法备份"
        exit 1
    fi
    log_info "ES 集群状态: ${status}"

    # 检查备份目录
    mkdir -p "${BACKUP_DIR}"
    if [[ ! -w "${BACKUP_DIR}" ]]; then
        log_error "备份目录不可写: ${BACKUP_DIR}"
        exit 1
    fi

    log_info "前置条件检查通过"
}

# ==========================================
# 注册备份仓库
# ==========================================
register_repository() {
    log_step "注册/检查备份仓库..."

    # 检查仓库是否已存在
    local repo_check
    repo_check=$(es_curl "GET" "/_snapshot/${REPO_NAME}" 2>/dev/null)

    if echo "${repo_check}" | jq -e '.error' &>/dev/null; then
        # 仓库不存在，创建
        log_info "创建备份仓库: ${REPO_NAME}"
        local result
        result=$(es_curl "PUT" "/_snapshot/${REPO_NAME}" "{
            \"type\": \"fs\",
            \"settings\": {
                \"location\": \"${BACKUP_DIR}\",
                \"compress\": true,
                \"max_snapshot_bytes_per_sec\": \"100mb\",
                \"max_restore_bytes_per_sec\": \"100mb\"
            }
        }")

        if echo "${result}" | jq -e '.acknowledged == true' &>/dev/null; then
            log_info "备份仓库创建成功"
        else
            log_error "备份仓库创建失败: ${result}"
            exit 1
        fi
    else
        log_info "备份仓库已存在: ${REPO_NAME}"
    fi

    # 验证仓库
    local verify_result
    verify_result=$(es_curl "POST" "/_snapshot/${REPO_NAME}/_verify" 2>/dev/null)
    if echo "${verify_result}" | jq -e '.nodes | length > 0' &>/dev/null; then
        log_info "备份仓库验证通过"
    else
        log_warn "备份仓库验证结果: ${verify_result}"
    fi
}

# ==========================================
# 执行快照备份
# ==========================================
do_backup() {
    log_step "开始 Elasticsearch 快照备份..."

    # 获取要备份的索引列表
    local indices
    indices=$(es_curl "GET" "/_cat/indices?h=index,health,status&format=json" 2>/dev/null)
    log_info "当前索引列表:"
    echo "${indices}" | jq -r '.[] | "  \(.index) - health: \(.health), status: \(.status)"' 2>/dev/null || echo "${indices}"

    # 获取匹配的索引
    local backup_indices
    backup_indices=$(es_curl "GET" "/_cat/indices/${ES_INDEX_PATTERNS}?h=index&format=json" 2>/dev/null | jq -r '.[].index' 2>/dev/null | paste -sd, -)

    if [[ -z "${backup_indices}" ]]; then
        log_warn "没有匹配 '${ES_INDEX_PATTERNS}' 的索引需要备份"
        backup_indices=""  # 备份所有索引
    else
        log_info "将备份索引: ${backup_indices}"
    fi

    # 创建快照
    local snapshot_body
    if [[ -n "${backup_indices}" ]]; then
        snapshot_body="{
            \"indices\": \"${backup_indices}\",
            \"ignore_unavailable\": true,
            \"include_global_state\": false,
            \"partial\": false
        }"
    else
        snapshot_body="{
            \"ignore_unavailable\": true,
            \"include_global_state\": false,
            \"partial\": false
        }"
    fi

    log_info "创建快照: ${SNAPSHOT_NAME}..."
    local create_result
    create_result=$(es_curl "PUT" "/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}?wait_for_completion=false" "${snapshot_body}")

    if echo "${create_result}" | jq -e '.accepted == true' &>/dev/null; then
        log_info "快照创建已接受"
    else
        log_error "快照创建失败: ${create_result}"
        exit 1
    fi

    # 等待快照完成
    log_info "等待快照完成..."
    local max_wait=60  # 最多等待10分钟
    local wait_count=0
    while [[ ${wait_count} -lt ${max_wait} ]]; do
        local snap_status
        snap_status=$(es_curl "GET" "/_snapshot/${REPO_NAME}/${SNAPSHOT_NAME}" 2>/dev/null)
        local state
        state=$(echo "${snap_status}" | jq -r '.snapshots[0].state' 2>/dev/null)

        case "${state}" in
            SUCCESS)
                log_info "快照创建成功!"
                # 输出快照详情
                local total_size
                total_size=$(echo "${snap_status}" | jq -r '.snapshots[0].meta.total_size' 2>/dev/null)
                local shard_count
                shard_count=$(echo "${snap_status}" | jq -r '.snapshots[0].shards.total' 2>/dev/null)
                log_info "快照大小: ${total_size}, 分片数: ${shard_count}"
                break
                ;;
            IN_PROGRESS)
                local progress
                progress=$(echo "${snap_status}" | jq -r '.snapshots[0].shards.stats.total' 2>/dev/null || echo "0")
                log_info "快照进行中... (${wait_count}/${max_wait})"
                sleep 10
                wait_count=$((wait_count + 1))
                ;;
            FAILED)
                log_error "快照创建失败!"
                log_error "$(echo "${snap_status}" | jq -r '.snapshots[0].failures' 2>/dev/null)"
                exit 1
                ;;
            PARTIAL)
                log_warn "快照部分完成 (部分分片失败)"
                break
                ;;
            *)
                log_info "快照状态: ${state}"
                sleep 10
                wait_count=$((wait_count + 1))
                ;;
        esac
    done

    if [[ ${wait_count} -ge ${max_wait} ]]; then
        log_error "快照创建超时!"
        exit 1
    fi

    log_info "Elasticsearch 快照备份完成: ${SNAPSHOT_NAME}"
    echo "${SNAPSHOT_NAME}"
}

# ==========================================
# 清理过期快照
# ==========================================
cleanup_old_snapshots() {
    log_step "清理 ${RETENTION_DAYS} 天前的过期快照..."

    # 获取所有快照列表
    local snapshots
    snapshots=$(es_curl "GET" "/_snapshot/${REPO_NAME}/_all" 2>/dev/null)
    local snap_count
    snap_count=$(echo "${snapshots}" | jq '.snapshots | length' 2>/dev/null || echo 0)

    if [[ "${snap_count}" -eq 0 ]]; then
        log_info "没有快照需要清理"
        return
    fi

    # 查找过期快照
    local cutoff_date
    cutoff_date=$(date -d "${RETENTION_DAYS} days ago" +%Y%m%d 2>/dev/null || date -v-${RETENTION_DAYS}d +%Y%m%d)

    local deleted_count=0
    echo "${snapshots}" | jq -r '.snapshots[] | "\(.snapshot) \(.start_time)"' 2>/dev/null | while read -r snap_name start_time; do
        # 提取快照日期
        local snap_date
        snap_date=$(echo "${snap_name}" | grep -oP '\d{8}' || echo "")

        if [[ -n "${snap_date}" && "${snap_date}" < "${cutoff_date}" ]]; then
            log_info "删除过期快照: ${snap_name}"
            local delete_result
            delete_result=$(es_curl "DELETE" "/_snapshot/${REPO_NAME}/${snap_name}")
            if echo "${delete_result}" | jq -e '.acknowledged == true' &>/dev/null; then
                log_info "快照 ${snap_name} 已删除"
            else
                log_warn "快照 ${snap_name} 删除失败: ${delete_result}"
            fi
        fi
    done

    log_info "快照清理完成"
}

# ==========================================
# 验证快照
# ==========================================
verify_snapshot() {
    local snapshot_name="$1"

    log_step "验证快照: ${snapshot_name}..."

    local status
    status=$(es_curl "GET" "/_snapshot/${REPO_NAME}/${snapshot_name}" 2>/dev/null)

    local state
    state=$(echo "${status}" | jq -r '.snapshots[0].state' 2>/dev/null)

    if [[ "${state}" == "SUCCESS" ]]; then
        log_info "快照状态: SUCCESS"

        # 检查分片完整性
        local total_shards
        total_shards=$(echo "${status}" | jq -r '.snapshots[0].shards.total' 2>/dev/null)
        local failed_shards
        failed_shards=$(echo "${status}" | jq -r '.snapshots[0].shards.failed' 2>/dev/null)

        if [[ "${failed_shards}" -eq 0 ]]; then
            log_info "所有分片完整 (${total_shards}/${total_shards})"
        else
            log_error "存在失败分片: ${failed_shards}/${total_shards}"
            return 1
        fi
    else
        log_error "快照状态异常: ${state}"
        return 1
    fi

    log_info "快照验证通过"
    return 0
}

# ==========================================
# 使用帮助
# ==========================================
usage() {
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  --verify <snapshot>  验证指定快照"
    echo "  --cleanup-only       仅清理过期快照"
    echo "  --dry-run            试运行"
    echo "  -h, --help           显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  ES_HOST              ES 地址 (默认: http://127.0.0.1:9200)"
    echo "  ES_USER              ES 用户名 (默认: elastic)"
    echo "  ES_PASSWORD          ES 密码"
    echo "  ES_INDEX_PATTERNS    索引模式 (默认: langtou_*)"
    echo "  BACKUP_DIR           备份目录 (默认: /data/backups/elasticsearch)"
    echo "  RETENTION_DAYS       保留天数 (默认: 7)"
}

# ==========================================
# 主入口
# ==========================================
main() {
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/es-backup.log"

    local dry_run=false
    local cleanup_only=false
    local verify_snapshot_name=""

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --verify)
                verify_snapshot_name="$2"
                shift 2
                ;;
            --cleanup-only)
                cleanup_only=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            -h|--help|help)
                usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
    done

    echo ""
    echo "=========================================="
    echo "  Langtou Elasticsearch 备份"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=========================================="
    echo ""

    if [[ -n "${verify_snapshot_name}" ]]; then
        check_prerequisites
        verify_snapshot "${verify_snapshot_name}"
        exit $?
    fi

    if [[ "${cleanup_only}" == "true" ]]; then
        check_prerequisites
        cleanup_old_snapshots
        exit 0
    fi

    if [[ "${dry_run}" == "true" ]]; then
        log_info "试运行模式"
        exit 0
    fi

    check_prerequisites
    register_repository
    local result
    result=$(do_backup)
    verify_snapshot "${result}"
    cleanup_old_snapshots

    log_info "Elasticsearch 备份流程完成"
}

main "$@"
