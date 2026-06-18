#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 一键启动脚本
# 支持 dev / prod 模式
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
ENV_FILE="$PROJECT_DIR/.env"

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

# 检查Docker和Docker Compose
check_docker() {
    log_info "检查 Docker 环境..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装,请先安装 Docker 24.x+"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose 未安装,请先安装 Docker Compose 2.x+"
        exit 1
    fi

    DOCKER_VERSION=$(docker --version | grep -oP '\d+\.\d+')
    log_success "Docker 版本: $DOCKER_VERSION"

    if docker compose version &> /dev/null; then
        COMPOSE_VERSION=$(docker compose version | grep -oP '\d+\.\d+')
        COMPOSE_CMD="docker compose"
    else
        COMPOSE_VERSION=$(docker-compose --version | grep -oP '\d+\.\d+')
        COMPOSE_CMD="docker-compose"
    fi
    log_success "Docker Compose 版本: $COMPOSE_VERSION"

    # 检查Docker是否运行
    if ! docker info &> /dev/null; then
        log_error "Docker 守护进程未运行,请启动 Docker"
        exit 1
    fi

    log_success "Docker 环境检查通过"
}

# 检查环境文件
check_env() {
    log_info "检查环境配置文件..."

    if [ ! -f "$ENV_FILE" ]; then
        log_warn "环境文件 $ENV_FILE 不存在,使用默认配置"
    else
        log_success "环境配置文件存在"
    fi
}

# 创建必要的目录
prepare_directories() {
    log_info "准备目录结构..."

    mkdir -p "$PROJECT_DIR/logs"
    mkdir -p "$PROJECT_DIR/data/mysql"
    mkdir -p "$PROJECT_DIR/data/redis"
    mkdir -p "$PROJECT_DIR/data/es"
    mkdir -p "$PROJECT_DIR/data/nacos"

    log_success "目录准备完成"
}

# 启动开发环境
start_dev() {
    log_info "启动开发环境 (docker-compose.dev.yml)..."

    cd "$PROJECT_DIR"
    $COMPOSE_CMD -f docker-compose.dev.yml up -d

    log_success "开发环境服务启动完成"
}

# 启动生产环境
start_prod() {
    log_info "启动生产环境 (docker-compose.yml)..."

    cd "$PROJECT_DIR"
    $COMPOSE_CMD -f docker-compose.yml up -d

    log_success "生产环境服务启动完成"
}

# 启动监控服务
start_monitoring() {
    log_info "启动监控服务..."

    cd "$PROJECT_DIR"
    $COMPOSE_CMD -f docker-compose.dev.yml --profile monitoring up -d

    log_success "监控服务启动完成"
}

# 查看服务状态
status() {
    cd "$PROJECT_DIR"

    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    fi

    local mode="${1:-dev}"
    if [ "$mode" = "prod" ]; then
        $COMPOSE_CMD -f docker-compose.yml ps
    else
        $COMPOSE_CMD -f docker-compose.dev.yml ps
    fi
}

# 查看日志
logs() {
    cd "$PROJECT_DIR"

    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    fi

    local mode="${1:-dev}"
    local service="${2:-}"

    if [ "$mode" = "prod" ]; then
        if [ -n "$service" ]; then
            $COMPOSE_CMD -f docker-compose.yml logs -f "$service"
        else
            $COMPOSE_CMD -f docker-compose.yml logs -f
        fi
    else
        if [ -n "$service" ]; then
            $COMPOSE_CMD -f docker-compose.dev.yml logs -f "$service"
        else
            $COMPOSE_CMD -f docker-compose.dev.yml logs -f
        fi
    fi
}

# 主函数
main() {
    local mode="${1:-dev}"
    local action="${2:-start}"

    case "$action" in
        start)
            check_docker
            check_env
            prepare_directories
            if [ "$mode" = "prod" ]; then
                start_prod
            else
                start_dev
            fi
            log_success "========================================"
            log_success "Langtou 所有服务启动完成!"
            log_success "========================================"
            echo ""
            log_info "服务访问地址:"
            echo "  - Gateway API:    http://localhost:8080"
            echo "  - Nacos Console:  http://localhost:8848/nacos"
            echo "  - MySQL:          localhost:3306"
            echo "  - Redis:          localhost:6379"
            echo "  - Kafka:          localhost:9092"
            echo "  - Elasticsearch:  http://localhost:9200"
            echo "  - Kibana:         http://localhost:5601"
            echo "  - Prometheus:     http://localhost:9090"
            echo "  - Grafana:        http://localhost:3000"
            echo ""
            log_info "查看服务状态:"
            echo "  $0 $mode status"
            echo ""
            log_info "查看日志:"
            echo "  $0 $mode logs [service-name]"
            ;;
        monitoring)
            check_docker
            start_monitoring
            ;;
        status)
            status "$mode"
            ;;
        logs)
            logs "$mode" "$3"
            ;;
        *)
            echo "用法: $0 [dev|prod] [start|monitoring|status|logs [service-name]]"
            echo ""
            echo "模式:"
            echo "  dev   - 开发环境 (默认)"
            echo "  prod  - 生产环境"
            echo ""
            echo "命令:"
            echo "  start       - 启动所有服务 (默认)"
            echo "  monitoring  - 启动监控服务 (仅dev)"
            echo "  status      - 查看服务状态"
            echo "  logs        - 查看日志"
            echo ""
            echo "示例:"
            echo "  $0 dev start                  # 启动开发环境"
            echo "  $0 prod start                 # 启动生产环境"
            echo "  $0 dev status                 # 查看开发环境状态"
            echo "  $0 dev logs langtou-mysql-dev # 查看MySQL日志"
            exit 1
            ;;
    esac
}

main "$@"
