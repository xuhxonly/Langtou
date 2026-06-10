#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 一键停止脚本
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

# 停止所有服务
stop_all() {
    log_info "开始停止 Langtou 所有服务..."
    
    cd "$PROJECT_DIR"
    
    # 检查docker compose命令
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        log_error "Docker Compose 不可用"
        exit 1
    fi
    
    # 停止所有服务
    log_info "停止所有容器..."
    $COMPOSE_CMD down
    
    log_success "========================================"
    log_success "Langtou 所有服务已停止!"
    log_success "========================================"
}

# 停止并清理数据
stop_clean() {
    log_warn "将停止所有服务并清理数据卷..."
    read -p "确认清理? (y/N): " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        cd "$PROJECT_DIR"
        
        if docker compose version &> /dev/null; then
            COMPOSE_CMD="docker compose"
        elif command -v docker-compose &> /dev/null; then
            COMPOSE_CMD="docker-compose"
        fi
        
        log_info "停止所有服务并清理数据卷..."
        $COMPOSE_CMD down -v
        
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
    cd "$PROJECT_DIR"
    
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    fi
    
    echo ""
    $COMPOSE_CMD ps
    echo ""
}

# 查看日志
logs() {
    cd "$PROJECT_DIR"
    
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    fi
    
    local service="${1:-}"
    
    if [ -n "$service" ]; then
        $COMPOSE_CMD logs -f "$service"
    else
        $COMPOSE_CMD logs -f
    fi
}

# 主函数
main() {
    case "${1:-stop}" in
        stop)
            stop_all
            ;;
        clean)
            stop_clean
            ;;
        status)
            status
            ;;
        logs)
            logs "$2"
            ;;
        *)
            echo "用法: $0 [stop|clean|status|logs [service-name]]"
            echo "  stop   - 停止所有服务 (默认)"
            echo "  clean  - 停止服务并清理数据卷"
            echo "  status - 查看服务状态"
            echo "  logs   - 查看日志"
            echo "  logs [service] - 查看指定服务日志"
            exit 1
            ;;
    esac
}

main "$@"
