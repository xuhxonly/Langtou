#!/bin/bash
# ==========================================
# Langtou - 统一备份调度脚本
# 功能: 统一调度 MySQL/Redis/ES 备份任务
# 用法: ./backup-cron.sh [--mysql] [--redis] [--es] [--all] [--dry-run]
# 建议通过 crontab 调度执行
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="/var/log/langtou/backup-cron.log"
LOCK_FILE="/var/run/langtou/backup-cron.lock"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 备份结果统计
TOTAL_SUCCESS=0
TOTAL_FAILED=0
FAILED_SERVICES=""

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

# ==========================================
# 锁机制 (防止并发执行)
# ==========================================
acquire_lock() {
    mkdir -p "$(dirname "${LOCK_FILE}")"
    if [[ -f "${LOCK_FILE}" ]]; then
        local pid
        pid=$(cat "${LOCK_FILE}")
        if kill -0 "${pid}" 2>/dev/null; then
            log_error "另一个备份进程正在运行 (PID: ${pid})"
            exit 1
        else
            log_warn "清理过期锁文件"
            rm -f "${LOCK_FILE}"
        fi
    fi
    echo $$ > "${LOCK_FILE}"
    trap 'rm -f "${LOCK_FILE}"' EXIT
}

# ==========================================
# MySQL 备份
# ==========================================
backup_mysql() {
    log_step "===== MySQL 备份 ====="

    local mysql_script="${SCRIPT_DIR}/mysql-backup.sh"
    if [[ ! -f "${mysql_script}" ]]; then
        log_error "MySQL 备份脚本不存在: ${mysql_script}"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} mysql"
        return 1
    fi

    chmod +x "${mysql_script}"

    if bash "${mysql_script}" >> "${LOG_FILE}" 2>&1; then
        log_info "MySQL 备份成功"
        TOTAL_SUCCESS=$((TOTAL_SUCCESS + 1))
    else
        log_error "MySQL 备份失败"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} mysql"
    fi
}

# ==========================================
# Redis 备份
# ==========================================
backup_redis() {
    log_step "===== Redis 备份 ====="

    local redis_script="${SCRIPT_DIR}/redis-backup.sh"
    if [[ ! -f "${redis_script}" ]]; then
        log_error "Redis 备份脚本不存在: ${redis_script}"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} redis"
        return 1
    fi

    chmod +x "${redis_script}"

    if bash "${redis_script}" >> "${LOG_FILE}" 2>&1; then
        log_info "Redis 备份成功"
        TOTAL_SUCCESS=$((TOTAL_SUCCESS + 1))
    else
        log_error "Redis 备份失败"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} redis"
    fi
}

# ==========================================
# Elasticsearch 备份
# ==========================================
backup_elasticsearch() {
    log_step "===== Elasticsearch 备份 ====="

    local es_script="${SCRIPT_DIR}/es-backup.sh"
    if [[ ! -f "${es_script}" ]]; then
        log_error "ES 备份脚本不存在: ${es_script}"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} elasticsearch"
        return 1
    fi

    chmod +x "${es_script}"

    if bash "${es_script}" >> "${LOG_FILE}" 2>&1; then
        log_info "Elasticsearch 备份成功"
        TOTAL_SUCCESS=$((TOTAL_SUCCESS + 1))
    else
        log_error "Elasticsearch 备份失败"
        TOTAL_FAILED=$((TOTAL_FAILED + 1))
        FAILED_SERVICES="${FAILED_SERVICES} elasticsearch"
    fi
}

# ==========================================
# 发送通知 (可选)
# ==========================================
send_notification() {
    local status="$1"
    local message="$2"

    # Webhook 通知 (可替换为企业微信/钉钉/Slack)
    local webhook_url="${NOTIFICATION_WEBHOOK_URL:-}"

    if [[ -n "${webhook_url}" ]]; then
        local payload
        payload=$(cat <<EOF
{
    "msgtype": "text",
    "text": {
        "content": "[Langtou 备份通知] ${status}\n时间: $(date '+%Y-%m-%d %H:%M:%S')\n${message}"
    }
}
EOF
)
        curl -s -X POST "${webhook_url}" \
            -H "Content-Type: application/json" \
            -d "${payload}" &>/dev/null || true
    fi

    # 邮件通知 (可选)
    local email_recipient="${NOTIFICATION_EMAIL:-}"
    if [[ -n "${email_recipient}" && -n "${status}" ]]; then
        echo "${message}" | mail -s "[Langtou 备份] ${status}" "${email_recipient}" 2>/dev/null || true
    fi
}

# ==========================================
# 使用帮助
# ==========================================
usage() {
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  --mysql        仅备份 MySQL"
    echo "  --redis        仅备份 Redis"
    echo "  --es           仅备份 Elasticsearch"
    echo "  --all          备份所有服务 (默认)"
    echo "  --dry-run      试运行"
    echo "  --setup-cron   自动配置 crontab 定时任务"
    echo "  -h, --help     显示帮助信息"
    echo ""
    echo "Crontab 配置示例:"
    echo "  # 每天凌晨2点执行全量备份"
    echo "  0 2 * * * ${SCRIPT_DIR}/backup-cron.sh --all >> ${LOG_FILE} 2>&1"
    echo ""
    echo "  # 每6小时备份一次"
    echo "  0 */6 * * * ${SCRIPT_DIR}/backup-cron.sh --all >> ${LOG_FILE} 2>&1"
    echo ""
    echo "  # MySQL 每天备份，Redis 每6小时备份"
    echo "  0 2 * * * ${SCRIPT_DIR}/backup-cron.sh --mysql >> ${LOG_FILE} 2>&1"
    echo "  0 */6 * * * ${SCRIPT_DIR}/backup-cron.sh --redis >> ${LOG_FILE} 2>&1"
    echo ""
    echo "环境变量:"
    echo "  NOTIFICATION_WEBHOOK_URL  通知 Webhook URL"
    echo "  NOTIFICATION_EMAIL        通知邮箱"
}

# ==========================================
# 配置 Crontab
# ==========================================
setup_cron() {
    log_step "配置 crontab 定时任务..."

    local cron_entry="0 2 * * * ${SCRIPT_DIR}/backup-cron.sh --all >> ${LOG_FILE} 2>&1"

    # 检查是否已存在
    if crontab -l 2>/dev/null | grep -q "backup-cron.sh"; then
        log_warn "crontab 中已存在备份任务，跳过"
    else
        (crontab -l 2>/dev/null; echo "${cron_entry}") | crontab -
        log_info "crontab 定时任务已添加: 每天凌晨2:00执行全量备份"
    fi

    log_info "当前 crontab:"
    crontab -l 2>/dev/null | grep "backup-cron" || echo "  (无)"
}

# ==========================================
# 主入口
# ==========================================
main() {
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/backup-cron.log"

    local do_mysql=true
    local do_redis=true
    local do_es=true
    local dry_run=false
    local setup_cron_flag=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --mysql)
                do_redis=false
                do_es=false
                shift
                ;;
            --redis)
                do_mysql=false
                do_es=false
                shift
                ;;
            --es)
                do_mysql=false
                do_redis=false
                shift
                ;;
            --all)
                do_mysql=true
                do_redis=true
                do_es=true
                shift
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --setup-cron)
                setup_cron_flag=true
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
    echo "  Langtou 统一备份调度"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "=========================================="
    echo ""

    if [[ "${setup_cron_flag}" == "true" ]]; then
        setup_cron
        exit 0
    fi

    if [[ "${dry_run}" == "true" ]]; then
        log_info "试运行模式"
        log_info "MySQL: ${do_mysql}, Redis: ${do_redis}, ES: ${do_es}"
        exit 0
    fi

    # 获取锁
    acquire_lock

    local start_time
    start_time=$(date +%s)

    # 执行备份
    if [[ "${do_mysql}" == "true" ]]; then
        backup_mysql
    fi

    if [[ "${do_redis}" == "true" ]]; then
        backup_redis
    fi

    if [[ "${do_es}" == "true" ]]; then
        backup_elasticsearch
    fi

    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))

    # 汇总报告
    echo ""
    echo "=========================================="
    echo "  备份汇总"
    echo "  成功: ${TOTAL_SUCCESS}, 失败: ${TOTAL_FAILED}"
    echo "  耗时: ${duration} 秒"
    echo "=========================================="
    echo ""

    # 发送通知
    if [[ "${TOTAL_FAILED}" -eq 0 ]]; then
        local msg="所有备份任务成功完成 (${TOTAL_SUCCESS}/${TOTAL_SUCCESS})，耗时 ${duration} 秒"
        log_info "${msg}"
        send_notification "SUCCESS" "${msg}"
    else
        local msg="备份任务部分失败! 成功: ${TOTAL_SUCCESS}, 失败: ${TOTAL_FAILED}。失败服务:${FAILED_SERVICES}"
        log_error "${msg}"
        send_notification "FAILURE" "${msg}"
        exit 1
    fi
}

main "$@"
