#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 一键停止脚本
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

# 获取 Compose 命令
get_compose_cmd() {
    if docker compose version &> /dev/null; then
        echo "docker compose"
    elif command -v docker-compose &> /dev/null; then
        echo "docker-compose"
    else
        log_error "Docker Compose 不可用"
        exit 1
    fi
}

# 停止所有服务
stop_all() {
    local mode="${1:-dev}"
    log_info "开始停止 Langtou $mode 环境服务..."

    cd "$PROJECT_DIR"
    local COMPOSE_CMD=$(get_compose_cmd)

    if [ "$mode" = "prod" ]; then
        $COMPOSE_CMD -f docker-compose.yml down
    else
        $COMPOSE_CMD -f docker-compose.dev.yml down
    fi

    log_success "========================================"
    log_success "Langtou $mode 环境服务已停止!"
    log_success "========================================"
}

# 停止并清理数据
stop_clean() {
    local mode="${1:-dev}"
    log_warn "将停止所有 $mode 服务并清理数据卷..."
    read -p "确认清理? (y/N): " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        cd "$PROJECT_DIR"
        local COMPOSE_CMD=$(get_compose_cmd)

        if [ "$mode" = "prod" ]; then
            $COMPOSE_CMD -f docker-compose.yml down -v
        else
            $COMPOSE_CMD -f docker-compose.dev.yml down -v
        fi

        # 清理未使用的镜像
        log_info "清理未使用的 Docker 资源..."
        docker system prune -f

        log_success "数据卷已清理!"
    else
        log_info "取消清理操作"
    fi
}

# 查看服务状态
status() {
    local mode="${1:-dev}"
    cd "$PROJECT_DIR"
    local COMPOSE_CMD=$(get_compose_cmd)

    echo ""
    if [ "$mode" = "prod" ]; then
        $COMPOSE_CMD -f docker-compose.yml ps
    else
        $COMPOSE_CMD -f docker-compose.dev.yml ps
    fi
    echo ""
}

# 查看日志
logs() {
    local mode="${1:-dev}"
    local service="${2:-}"

    cd "$PROJECT_DIR"
    local COMPOSE_CMD=$(get_compose_cmd)

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
    local action="${2:-stop}"

    case "$action" in
        stop)
            stop_all "$mode"
            ;;
        clean)
            stop_clean "$mode"
            ;;
        status)
            status "$mode"
            ;;
        logs)
            logs "$mode" "$3"
            ;;
        *)
            echo "用法: $0 [dev|prod] [stop|clean|status|logs [service-name]]"
            echo ""
            echo "模式:"
            echo "  dev   - 开发环境 (默认)"
            echo "  prod  - 生产环境"
            echo ""
            echo "命令:"
            echo "  stop   - 停止所有服务 (默认)"
            echo "  clean  - 停止服务并清理数据卷"
            echo "  status - 查看服务状态"
            echo "  logs   - 查看日志"
            echo ""
            echo "示例:"
            echo "  $0 dev stop                   # 停止开发环境"
            echo "  $0 prod stop                  # 停止生产环境"
            echo "  $0 dev clean                  # 清理开发环境数据"
            echo "  $0 dev logs langtou-mysql-dev # 查看MySQL日志"
            exit 1
            ;;
    esac
}

main "$@"
