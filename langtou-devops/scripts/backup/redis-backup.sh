#!/bin/bash
# ==========================================
# Langtou - Redis RDB 备份脚本
# 功能: 触发 Redis RDB 快照备份，保留7天
# 用法: ./redis-backup.sh [--verify <file>] [--cleanup-only] [--dry-run]
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_REPLICA="${REDIS_REPLICA:-false}"  # 是否从从库备份

BACKUP_DIR="/data/backups/redis"
RETENTION_DAYS=7
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_PREFIX="langtou_redis"
LOG_FILE="/var/log/langtou/redis-backup.log"

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

# Redis CLI 命令封装
redis_cli() {
    local cmd="$1"
    if [[ -n "${REDIS_PASSWORD}" ]]; then
        redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" -a "${REDIS_PASSWORD}" --no-auth-warning "${cmd}"
    else
        redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT}" "${cmd}"
    fi
}

# ==========================================
# 前置检查
# ==========================================
check_prerequisites() {
    log_step "检查前置条件..."

    # 检查 redis-cli
    if ! command -v redis-cli &>/dev/null; then
        log_error "redis-cli 未安装"
        exit 1
    fi

    # 检查 Redis 连接
    local pong
    pong=$(redis_cli "PING" 2>/dev/null) || {
        log_error "无法连接到 Redis: ${REDIS_HOST}:${REDIS_PORT}"
        exit 1
    }
    if [[ "${pong}" != "PONG" ]]; then
        log_error "Redis 响应异常: ${pong}"
        exit 1
    fi

    # 检查是否从库 (建议从从库备份)
    local role
    role=$(redis_cli "ROLE" 2>/dev/null | head -1)
    if [[ "${role}" == "slave" ]]; then
        log_info "当前为从库，适合备份"
    elif [[ "${role}" == "master" ]]; then
        if [[ "${REDIS_REPLICA}" != "true" ]]; then
            log_warn "当前为主库，建议从从库备份以避免阻塞"
        fi
    fi

    # 检查备份目录
    mkdir -p "${BACKUP_DIR}"
    if [[ ! -w "${BACKUP_DIR}" ]]; then
        log_error "备份目录不可写: ${BACKUP_DIR}"
        exit 1
    fi

    log_info "前置条件检查通过"
}

# ==========================================
# 执行 RDB 备份
# ==========================================
do_backup() {
    log_step "开始 Redis RDB 备份..."

    # 获取 Redis 信息
    local db_size
    db_size=$(redis_cli "DBSIZE" | awk '{print $2}')
    log_info "当前数据库 Key 数量: ${db_size}"

    local used_memory
    used_memory=$(redis_cli "INFO memory" 2>/dev/null | grep "used_memory_human:" | awk -F: '{print $2}' | tr -d '\r')
    log_info "内存使用: ${used_memory}"

    # 获取 Redis 配置的 dump 路径
    local redis_dir
    redis_dir=$(redis_cli "CONFIG GET dir" | tail -1 | tr -d '\r')
    local redis_dbfilename
    redis_dbfilename=$(redis_cli "CONFIG GET dbfilename" | tail -1 | tr -d '\r')
    local redis_rdb_path="${redis_dir}/${redis_dbfilename}"

    log_info "Redis RDB 路径: ${redis_rdb_path}"

    # 触发 BGSAVE (后台保存)
    log_info "触发 BGSAVE..."
    local bgsave_result
    bgsave_result=$(redis_cli "BGSAVE" 2>/dev/null)

    if [[ "${bgsave_result}" != "Background saving started" ]]; then
        # 检查是否已有 BGSAVE 在进行
        local last_bgsave_status
        last_bgsave_status=$(redis_cli "INFO persistence" 2>/dev/null | grep "rdb_bgsave_in_progress:" | awk -F: '{print $2}' | tr -d '\r')
        if [[ "${last_bgsave_status}" == "1" ]]; then
            log_warn "已有 BGSAVE 在进行中，等待完成..."
        else
            log_error "BGSAVE 失败: ${bgsave_result}"
            exit 1
        fi
    fi

    # 等待 BGSAVE 完成 (最多等待 300 秒)
    log_info "等待 BGSAVE 完成..."
    local max_wait=30
    local wait_count=0
    while [[ ${wait_count} -lt ${max_wait} ]]; do
        local save_status
        save_status=$(redis_cli "INFO persistence" 2>/dev/null | grep "rdb_bgsave_in_progress:" | awk -F: '{print $2}' | tr -d '\r')
        if [[ "${save_status}" == "0" ]]; then
            log_info "BGSAVE 完成"
            break
        fi
        sleep 10
        wait_count=$((wait_count + 1))
        log_info "等待中... (${wait_count}/${max_wait})"
    done

    if [[ ${wait_count} -ge ${max_wait} ]]; then
        log_error "BGSAVE 超时!"
        exit 1
    fi

    # 检查 RDB 文件
    if [[ ! -f "${redis_rdb_path}" ]]; then
        log_error "RDB 文件不存在: ${redis_rdb_path}"
        exit 1
    fi

    # 复制 RDB 文件到备份目录
    local backup_file="${BACKUP_DIR}/${BACKUP_PREFIX}_${BACKUP_DATE}.rdb"
    cp "${redis_rdb_path}" "${backup_file}"

    # 压缩
    log_info "压缩备份文件..."
    gzip -f "${backup_file}"
    local compressed_file="${backup_file}.gz"

    local original_size
    original_size=$(stat -c%s "${redis_rdb_path}" 2>/dev/null || stat -f%z "${redis_rdb_path}")
    local compressed_size
    compressed_size=$(stat -c%s "${compressed_file}" 2>/dev/null || stat -f%z "${compressed_file}")
    log_info "原始大小: $(numfmt --to=iec --suffix=B "${original_size}")"
    log_info "压缩后: $(numfmt --to=iec --suffix=B "${compressed_size}")"

    # 计算校验和
    local checksum
    checksum=$(sha256sum "${compressed_file}" | awk '{print $1}')
    echo "${checksum}  ${compressed_file}" > "${compressed_file}.sha256"
    log_info "SHA256: ${checksum}"

    log_info "Redis 备份完成: ${compressed_file}"
    echo "${compressed_file}"
}

# ==========================================
# 清理过期备份
# ==========================================
cleanup_old_backups() {
    log_step "清理 ${RETENTION_DAYS} 天前的过期备份..."

    local deleted_count=0
    while IFS= read -r -d '' old_file; do
        rm -f "${old_file}" "${old_file}.sha256"
        deleted_count=$((deleted_count + 1))
        log_info "已删除: $(basename "${old_file}")"
    done < <(find "${BACKUP_DIR}" -name "${BACKUP_PREFIX}_*" -mtime +${RETENTION_DAYS} -print0 2>/dev/null)

    if [[ "${deleted_count}" -gt 0 ]]; then
        log_info "清理完成: 删除 ${deleted_count} 个文件"
    else
        log_info "无需清理"
    fi
}

# ==========================================
# 验证备份
# ==========================================
verify_backup() {
    local backup_file="$1"

    log_step "验证 Redis 备份文件..."

    if [[ ! -f "${backup_file}" ]]; then
        log_error "备份文件不存在: ${backup_file}"
        return 1
    fi

    # SHA256 校验
    if [[ -f "${backup_file}.sha256" ]]; then
        if sha256sum -c "${backup_file}.sha256" --quiet 2>/dev/null; then
            log_info "SHA256 校验通过"
        else
            log_error "SHA256 校验失败!"
            return 1
        fi
    fi

    # gzip 完整性检查
    if [[ "${backup_file}" == *.gz ]]; then
        if gzip -t "${backup_file}" 2>/dev/null; then
            log_info "gzip 完整性验证通过"
        else
            log_error "gzip 文件损坏!"
            return 1
        fi
    fi

    log_info "备份验证通过"
    return 0
}

# ==========================================
# 使用帮助
# ==========================================
usage() {
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  --verify <file>     验证备份文件"
    echo "  --cleanup-only      仅清理过期备份"
    echo "  --dry-run           试运行"
    echo "  -h, --help          显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  REDIS_HOST          Redis 主机 (默认: 127.0.0.1)"
    echo "  REDIS_PORT          Redis 端口 (默认: 6379)"
    echo "  REDIS_PASSWORD      Redis 密码"
    echo "  BACKUP_DIR          备份目录 (默认: /data/backups/redis)"
    echo "  RETENTION_DAYS      保留天数 (默认: 7)"
}

# ==========================================
# 主入口
# ==========================================
main() {
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/redis-backup.log"

    local dry_run=false
    local cleanup_only=false
    local verify_file=""

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --verify)
                verify_file="$2"
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
    echo "  Langtou Redis 备份"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=========================================="
    echo ""

    if [[ -n "${verify_file}" ]]; then
        verify_backup "${verify_file}"
        exit $?
    fi

    if [[ "${cleanup_only}" == "true" ]]; then
        cleanup_old_backups
        exit 0
    fi

    if [[ "${dry_run}" == "true" ]]; then
        log_info "试运行模式"
        exit 0
    fi

    check_prerequisites
    local result_file
    result_file=$(do_backup)
    verify_backup "${result_file}"
    cleanup_old_backups

    log_info "Redis 备份流程完成"
}

main "$@"
