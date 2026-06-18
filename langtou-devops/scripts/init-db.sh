#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 数据库初始化脚本
# 使用 database/schema.sql 和 database/data.sql
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 数据库配置（优先从 .env 读取，否则使用默认值）
if [ -f "$PROJECT_DIR/.env" ]; then
    export $(grep -v '^#' "$PROJECT_DIR/.env" | xargs)
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_ROOT_USER="${DB_ROOT_USER:-root}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-${MYSQL_ROOT_PASSWORD:-CHANGE_ME}}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-langtou}}"
DB_USER="${DB_USER:-${MYSQL_USER:-langtou}}"
DB_PASSWORD="${DB_PASSWORD:-${MYSQL_PASSWORD:-CHANGE_ME}}"

# SQL 文件路径
SCHEMA_FILE="${PROJECT_DIR}/../langtou-database/schema.sql"
DATA_FILE="${PROJECT_DIR}/../langtou-database/data.sql"

# 打印带颜色的信息
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查SQL文件
check_sql_files() {
    log_info "检查 SQL 文件..."

    if [ ! -f "$SCHEMA_FILE" ]; then
        log_error "Schema 文件不存在: $SCHEMA_FILE"
        exit 1
    fi

    if [ ! -f "$DATA_FILE" ]; then
        log_warn "Data 文件不存在: $DATA_FILE"
    fi

    log_success "SQL 文件检查通过"
}

# 检查MySQL连接
check_mysql() {
    log_info "检查 MySQL 连接..."

    if ! command -v mysql &> /dev/null; then
        log_error "MySQL 客户端未安装"
        exit 1
    fi

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" -e "SELECT 1" &> /dev/null; then
            log_success "MySQL 连接成功"
            return 0
        fi

        log_warn "等待 MySQL 就绪... (尝试 $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done

    log_error "无法连接到 MySQL"
    exit 1
}

# 创建数据库和用户
init_database() {
    log_info "初始化数据库..."

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" <<EOF
-- 创建数据库
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 创建业务用户
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';

-- 创建Nacos数据库
CREATE DATABASE IF NOT EXISTS \`nacos\`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON \`nacos\`.* TO '${DB_USER}'@'%';

FLUSH PRIVILEGES;
EOF

    log_success "数据库和用户创建完成"
}

# 导入Schema
import_schema() {
    log_info "导入数据库 Schema..."

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" "$DB_NAME" < "$SCHEMA_FILE"

    log_success "Schema 导入完成"
}

# 导入数据
import_data() {
    if [ ! -f "$DATA_FILE" ]; then
        log_warn "跳过数据导入，文件不存在"
        return 0
    fi

    log_info "导入初始化数据..."

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" "$DB_NAME" < "$DATA_FILE"

    log_success "数据导入完成"
}

# 主函数
main() {
    log_info "========================================"
    log_info "Langtou 数据库初始化"
    log_info "========================================"

    check_sql_files
    check_mysql
    init_database
    import_schema
    import_data

    log_success "========================================"
    log_success "数据库初始化完成!"
    log_success "========================================"
    echo ""
    log_info "数据库信息:"
    echo "  - 主机: $DB_HOST:$DB_PORT"
    echo "  - 数据库: $DB_NAME"
    echo "  - 用户: $DB_USER"
    echo "  - Schema: $SCHEMA_FILE"
    echo "  - Data: $DATA_FILE"
    echo ""
}

main "$@"
