#!/bin/bash
# ==========================================
# Langtou - MySQL 数据库备份脚本
# 功能: mysqldump 定时备份，保留7天，压缩加密
# 用法: ./mysql-backup.sh [--full] [--tables <db.table,...>] [--dry-run]
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
# MySQL 连接配置 (建议通过环境变量或K8s Secret注入)
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"  # 必须设置，否则提示输入
MYSQL_DATABASE="${MYSQL_DATABASE:-langtou}"

# 备份配置
BACKUP_DIR="/data/backups/mysql"
RETENTION_DAYS=7
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_PREFIX="langtou_mysql"
LOG_FILE="/var/log/langtou/mysql-backup.log"

# 加密配置 (AES-256)
ENCRYPT_KEY="${ENCRYPT_KEY:-}"  # 32字节加密密钥，建议通过环境变量注入
ENCRYPT_ENABLED="${ENCRYPT_ENABLED:-true}"

# 压缩配置
COMPRESS_ENABLED=true

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

# ==========================================
# 前置检查
# ==========================================
check_prerequisites() {
    log_step "检查前置条件..."

    # 检查 mysqldump
    if ! command -v mysqldump &>/dev/null; then
        log_error "mysqldump 未安装，请安装 MySQL 客户端工具"
        exit 1
    fi

    # 检查 MySQL 连接
    if ! mysqladmin ping -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" \
        ${MYSQL_PASSWORD:+-p"${MYSQL_PASSWORD}"} --silent 2>/dev/null; then
        log_error "无法连接到 MySQL: ${MYSQL_HOST}:${MYSQL_PORT}"
        exit 1
    fi

    # 检查备份目录
    mkdir -p "${BACKUP_DIR}"
    if [[ ! -w "${BACKUP_DIR}" ]]; then
        log_error "备份目录不可写: ${BACKUP_DIR}"
        exit 1
    fi

    # 检查加密密钥
    if [[ "${ENCRYPT_ENABLED}" == "true" && -z "${ENCRYPT_KEY}" ]]; then
        log_warn "未设置 ENCRYPT_KEY，备份将不加密"
        ENCRYPT_ENABLED=false
    fi

    log_info "前置条件检查通过"
}

# ==========================================
# 执行备份
# ==========================================
do_backup() {
    local backup_type="${1:-database}"
    local tables="${2:-}"
    local backup_file="${BACKUP_DIR}/${BACKUP_PREFIX}_${backup_type}_${BACKUP_DATE}.sql"
    local final_file="${backup_file}"

    log_step "开始 MySQL ${backup_type} 备份..."

    # 构建 mysqldump 命令
    local dump_opts=(
        --host="${MYSQL_HOST}"
        --port="${MYSQL_PORT}"
        --user="${MYSQL_USER}"
        --single-transaction
        --routines
        --triggers
        --events
        --set-gtid-purged=OFF
        --max-allowed-packet=256M
        --net-buffer-length=32768
    )

    if [[ -n "${MYSQL_PASSWORD}" ]]; then
        dump_opts+=(--password="${MYSQL_PASSWORD}")
    fi

    case "${backup_type}" in
        full)
            # 全库备份
            dump_opts+=(--all-databases)
            log_info "执行全库备份..."
            mysqldump "${dump_opts[@]}" > "${backup_file}" 2>>"${LOG_FILE}"
            ;;
        database)
            # 单库备份
            dump_opts+=(--databases "${MYSQL_DATABASE}")
            log_info "备份数据库: ${MYSQL_DATABASE}..."
            mysqldump "${dump_opts[@]}" > "${backup_file}" 2>>"${LOG_FILE}"
            ;;
        tables)
            # 指定表备份
            if [[ -z "${tables}" ]]; then
                log_error "指定表备份需要 --tables 参数"
                exit 1
            fi
            log_info "备份表: ${tables}..."
            mysqldump "${dump_opts[@]}" "${MYSQL_DATABASE}" ${tables//,/ } > "${backup_file}" 2>>"${LOG_FILE}"
            ;;
        *)
            log_error "未知备份类型: ${backup_type}"
            exit 1
            ;;
    esac

    # 检查备份文件
    if [[ ! -s "${backup_file}" ]]; then
        log_error "备份文件为空或不存在!"
        exit 1
    fi

    local original_size
    original_size=$(stat -c%s "${backup_file}" 2>/dev/null || stat -f%z "${backup_file}")
    log_info "原始备份大小: $(numfmt --to=iec --suffix=B "${original_size}")"

    # 压缩
    if [[ "${COMPRESS_ENABLED}" == "true" ]]; then
        log_info "压缩备份文件..."
        gzip -f "${backup_file}"
        final_file="${backup_file}.gz"
        local compressed_size
        compressed_size=$(stat -c%s "${final_file}" 2>/dev/null || stat -f%z "${final_file}")
        local ratio
        ratio=$(echo "scale=1; ${compressed_size} * 100 / ${original_size}" | bc)
        log_info "压缩后大小: $(numfmt --to=iec --suffix=B "${compressed_size}") (压缩率: ${ratio}%)"
    fi

    # 加密
    if [[ "${ENCRYPT_ENABLED}" == "true" ]]; then
        log_info "加密备份文件..."
        local encrypted_file="${final_file}.enc"
        openssl enc -aes-256-cbc \
            -salt \
            -pbkdf2 \
            -iter 100000 \
            -in "${final_file}" \
            -out "${encrypted_file}" \
            -pass "pass:${ENCRYPT_KEY}"
        # 删除未加密文件
        rm -f "${final_file}"
        final_file="${encrypted_file}"
        log_info "备份文件已加密"
    fi

    # 计算校验和
    local checksum
    checksum=$(sha256sum "${final_file}" | awk '{print $1}')
    echo "${checksum}  ${final_file}" > "${final_file}.sha256"
    log_info "SHA256: ${checksum}"

    log_info "备份完成: ${final_file}"
    echo "${final_file}"
}

# ==========================================
# 清理过期备份
# ==========================================
cleanup_old_backups() {
    log_step "清理 ${RETENTION_DAYS} 天前的过期备份..."

    local deleted_count=0
    local total_freed=0

    while IFS= read -r -d '' old_file; do
        local file_size
        file_size=$(stat -c%s "${old_file}" 2>/dev/null || stat -f%z "${old_file}" || echo 0)
        rm -f "${old_file}" "${old_file}.sha256"
        deleted_count=$((deleted_count + 1))
        total_freed=$((total_freed + file_size))
        log_info "已删除: ${old_file}"
    done < <(find "${BACKUP_DIR}" -name "${BACKUP_PREFIX}_*" -mtime +${RETENTION_DAYS} -print0 2>/dev/null)

    if [[ "${deleted_count}" -gt 0 ]]; then
        log_info "清理完成: 删除 ${deleted_count} 个文件，释放 $(numfmt --to=iec --suffix=B "${total_freed}")"
    else
        log_info "无需清理"
    fi
}

# ==========================================
# 验证备份
# ==========================================
verify_backup() {
    local backup_file="$1"

    log_step "验证备份文件..."

    if [[ ! -f "${backup_file}" ]]; then
        log_error "备份文件不存在: ${backup_file}"
        return 1
    fi

    # 校验 SHA256
    if [[ -f "${backup_file}.sha256" ]]; then
        log_info "验证 SHA256 校验和..."
        if sha256sum -c "${backup_file}.sha256" --quiet 2>/dev/null; then
            log_info "SHA256 校验通过"
        else
            log_error "SHA256 校验失败! 备份文件可能已损坏"
            return 1
        fi
    fi

    # 验证备份内容 (解密+解压后检查)
    local temp_file="${backup_file}.verify.tmp"

    if [[ "${backup_file}" == *.enc ]]; then
        # 解密
        openssl enc -aes-256-cbc -d \
            -pbkdf2 -iter 100000 \
            -in "${backup_file}" \
            -out "${temp_file}" \
            -pass "pass:${ENCRYPT_KEY}" 2>/dev/null || {
            log_error "解密失败"
            return 1
        }
        backup_file="${temp_file}"
    fi

    if [[ "${backup_file}" == *.gz ]]; then
        # 解压验证
        if gzip -t "${backup_file}" 2>/dev/null; then
            log_info "gzip 完整性验证通过"
        else
            log_error "gzip 文件损坏!"
            rm -f "${temp_file}"
            return 1
        fi
    fi

    # 清理临时文件
    rm -f "${temp_file}"

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
    echo "  --full              全库备份"
    echo "  --database          单库备份 (默认)"
    echo "  --tables <list>     指定表备份 (逗号分隔)"
    echo "  --cleanup-only      仅清理过期备份"
    echo "  --verify <file>     验证备份文件"
    echo "  --dry-run           试运行 (不实际执行)"
    echo "  --no-compress       不压缩"
    echo "  --no-encrypt        不加密"
    echo "  -h, --help          显示帮助信息"
    echo ""
    echo "环境变量:"
    echo "  MYSQL_HOST          MySQL 主机 (默认: 127.0.0.1)"
    echo "  MYSQL_PORT          MySQL 端口 (默认: 3306)"
    echo "  MYSQL_USER          MySQL 用户 (默认: root)"
    echo "  MYSQL_PASSWORD      MySQL 密码"
    echo "  MYSQL_DATABASE      数据库名 (默认: langtou)"
    echo "  ENCRYPT_KEY         AES-256 加密密钥"
    echo "  BACKUP_DIR          备份目录 (默认: /data/backups/mysql)"
    echo "  RETENTION_DAYS      保留天数 (默认: 7)"
    echo ""
    echo "示例:"
    echo "  $0                              # 单库备份"
    echo "  $0 --full                       # 全库备份"
    echo "  $0 --tables user,post,comment   # 指定表备份"
    echo "  MYSQL_PASSWORD=xxx $0 --full    # 指定密码备份"
}

# ==========================================
# 主入口
# ==========================================
main() {
    # 创建日志目录
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/mysql-backup.log"

    local backup_type="database"
    local tables=""
    local dry_run=false
    local cleanup_only=false
    local verify_file=""

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --full)
                backup_type="full"
                shift
                ;;
            --database)
                backup_type="database"
                shift
                ;;
            --tables)
                backup_type="tables"
                tables="$2"
                shift 2
                ;;
            --cleanup-only)
                cleanup_only=true
                shift
                ;;
            --verify)
                verify_file="$2"
                shift 2
                ;;
            --dry-run)
                dry_run=true
                shift
                ;;
            --no-compress)
                COMPRESS_ENABLED=false
                shift
                ;;
            --no-encrypt)
                ENCRYPT_ENABLED=false
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
    echo "  Langtou MySQL 备份"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  类型: ${backup_type}"
    echo "=========================================="
    echo ""

    # 验证模式
    if [[ -n "${verify_file}" ]]; then
        verify_backup "${verify_file}"
        exit $?
    fi

    # 仅清理模式
    if [[ "${cleanup_only}" == "true" ]]; then
        cleanup_old_backups
        exit 0
    fi

    # 前置检查
    check_prerequisites

    # 试运行
    if [[ "${dry_run}" == "true" ]]; then
        log_info "试运行模式，不执行实际备份"
        log_info "备份类型: ${backup_type}"
        log_info "备份目录: ${BACKUP_DIR}"
        log_info "压缩: ${COMPRESS_ENABLED}"
        log_info "加密: ${ENCRYPT_ENABLED}"
        log_info "保留天数: ${RETENTION_DAYS}"
        exit 0
    fi

    # 执行备份
    local result_file
    result_file=$(do_backup "${backup_type}" "${tables}")

    # 验证备份
    verify_backup "${result_file}"

    # 清理过期备份
    cleanup_old_backups

    log_info "MySQL 备份流程完成"
}

main "$@"
