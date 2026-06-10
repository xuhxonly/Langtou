#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 一键启动脚本
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

# 启动基础服务
start_infra() {
    log_info "启动基础设施服务..."
    
    cd "$PROJECT_DIR"
    
    # 启动MySQL
    log_info "启动 MySQL..."
    $COMPOSE_CMD up -d mysql-master mysql-slave
    
    # 等待MySQL启动
    log_info "等待 MySQL 启动..."
    sleep 10
    
    # 启动Redis Cluster
    log_info "启动 Redis Cluster..."
    $COMPOSE_CMD up -d redis-node-1 redis-node-2 redis-node-3 redis-node-4 redis-node-5 redis-node-6
    
    # 初始化Redis Cluster
    log_info "初始化 Redis Cluster..."
    sleep 5
    $COMPOSE_CMD up -d redis-cluster-init
    
    # 启动Zookeeper和Kafka
    log_info "启动 Zookeeper 和 Kafka..."
    $COMPOSE_CMD up -d zookeeper kafka
    
    # 启动Elasticsearch和Kibana
    log_info "启动 Elasticsearch 和 Kibana..."
    $COMPOSE_CMD up -d elasticsearch kibana
    
    # 启动Nacos
    log_info "启动 Nacos..."
    $COMPOSE_CMD up -d nacos
    
    log_success "基础设施服务启动完成"
}

# 启动监控服务
start_monitoring() {
    log_info "启动监控服务..."
    
    cd "$PROJECT_DIR"
    $COMPOSE_CMD up -d prometheus grafana
    
    log_success "监控服务启动完成"
}

# 启动业务服务
start_services() {
    log_info "启动业务微服务..."
    
    cd "$PROJECT_DIR"
    
    # 等待基础设施就绪
    log_info "等待基础设施就绪..."
    sleep 30
    
    # 启动Gateway
    log_info "启动 Gateway..."
    $COMPOSE_CMD up -d gateway
    
    # 启动业务服务
    log_info "启动业务服务..."
    $COMPOSE_CMD up -d user-service content-service interact-service message-service
    
    log_success "业务微服务启动完成"
}

# 启动所有服务
start_all() {
    log_info "开始启动 Langtou 所有服务..."
    
    check_docker
    check_env
    prepare_directories
    start_infra
    start_monitoring
    start_services
    
    log_success "========================================"
    log_success "Langtou 所有服务启动完成!"
    log_success "========================================"
    echo ""
    log_info "服务访问地址:"
    echo "  - Gateway API:    http://localhost:8080"
    echo "  - Nacos Console:  http://localhost:8848/nacos"
    echo "  - MySQL:          localhost:3306 (root/Langtou@Root2024)"
    echo "  - Redis Cluster:  localhost:6379 (Langtou@Redis2024)"
    echo "  - Kafka:          localhost:9092"
    echo "  - Elasticsearch:  http://localhost:9200"
    echo "  - Kibana:         http://localhost:5601"
    echo "  - Prometheus:     http://localhost:9090"
    echo "  - Grafana:        http://localhost:3000 (admin/Langtou@Grafana2024)"
    echo ""
    log_info "查看服务状态:"
    echo "  docker compose ps"
    echo ""
    log_info "查看日志:"
    echo "  docker compose logs -f [service-name]"
}

# 主函数
main() {
    case "${1:-all}" in
        infra)
            check_docker
            check_env
            prepare_directories
            start_infra
            ;;
        monitoring)
            check_docker
            start_monitoring
            ;;
        services)
            check_docker
            start_services
            ;;
        all)
            start_all
            ;;
        *)
            echo "用法: $0 [infra|monitoring|services|all]"
            echo "  infra       - 仅启动基础设施"
            echo "  monitoring  - 仅启动监控服务"
            echo "  services    - 仅启动业务服务"
            echo "  all         - 启动所有服务 (默认)"
            exit 1
            ;;
    esac
}

main "$@"
