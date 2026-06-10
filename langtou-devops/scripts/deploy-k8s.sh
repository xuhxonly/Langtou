#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - K8s 部署脚本
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
K8S_DIR="$PROJECT_DIR/k8s"

# 默认环境
ENVIRONMENT="${1:-dev}"
IMAGE_TAG="${2:-latest}"

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

# 检查kubectl
check_kubectl() {
    log_info "检查 kubectl..."
    
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安装"
        exit 1
    fi
    
    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
    
    K8S_VERSION=$(kubectl version --client -o json | grep -oP '"gitVersion": "v\K[0-9]+\.[0-9]+')
    log_success "kubectl 版本: $K8S_VERSION"
    log_success "Kubernetes 集群连接正常"
}

# 部署命名空间
deploy_namespace() {
    log_info "部署命名空间..."
    kubectl apply -f "$K8S_DIR/namespace.yml"
    log_success "命名空间部署完成"
}

# 部署ConfigMap
deploy_configmap() {
    log_info "部署 ConfigMap..."
    kubectl apply -f "$K8S_DIR/configmap.yml"
    log_success "ConfigMap 部署完成"
}

# 部署Secret
deploy_secret() {
    log_info "部署 Secret..."
    kubectl apply -f "$K8S_DIR/secret.yml"
    log_success "Secret 部署完成"
}

# 部署基础资源
deploy_base() {
    log_info "部署基础资源..."
    deploy_namespace
    deploy_configmap
    deploy_secret
}

# 部署服务
deploy_services() {
    log_info "部署微服务..."
    
    local services=("gateway" "user-service" "content-service" "interact-service" "message-service")
    
    for service in "${services[@]}"; do
        log_info "部署 $service..."
        
        # 使用临时文件替换镜像标签
        local temp_file="/tmp/${service}-deploy.yml"
        sed "s|image: registry.langtou.local/langtou/${service}:latest|image: registry.langtou.local/langtou/${service}:${IMAGE_TAG}|g" \
            "$K8S_DIR/services/${service}.yml" > "$temp_file"
        
        kubectl apply -f "$temp_file"
        rm -f "$temp_file"
        
        log_success "$service 部署完成"
    done
}

# 部署Ingress
deploy_ingress() {
    log_info "部署 Ingress..."
    kubectl apply -f "$K8S_DIR/ingress/ingress.yml"
    log_success "Ingress 部署完成"
}

# 等待服务就绪
wait_for_services() {
    log_info "等待服务就绪..."
    
    local services=("gateway" "user-service" "content-service" "interact-service" "message-service")
    
    for service in "${services[@]}"; do
        log_info "等待 $service 就绪..."
        kubectl rollout status deployment/$service -n langtou --timeout=300s
    done
    
    log_success "所有服务已就绪"
}

# 完整部署
full_deploy() {
    log_info "========================================"
    log_info "开始部署 Langtou 到 Kubernetes"
    log_info "环境: $ENVIRONMENT"
    log_info "镜像标签: $IMAGE_TAG"
    log_info "========================================"
    
    check_kubectl
    deploy_base
    deploy_services
    deploy_ingress
    wait_for_services
    
    log_success "========================================"
    log_success "Langtou K8s 部署完成!"
    log_success "========================================"
    echo ""
    log_info "部署状态:"
    kubectl get pods -n langtou
    echo ""
    log_info "服务列表:"
    kubectl get svc -n langtou
    echo ""
    log_info "Ingress:"
    kubectl get ingress -n langtou
}

# 滚动更新
rolling_update() {
    local service="$1"
    local tag="${2:-$IMAGE_TAG}"
    
    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        exit 1
    fi
    
    log_info "滚动更新 $service 到镜像标签 $tag..."
    kubectl set image deployment/$service \
        $service=registry.langtou.local/langtou/$service:$tag \
        -n langtou
    
    kubectl rollout status deployment/$service -n langtou --timeout=300s
    log_success "$service 更新完成"
}

# 回滚
rollback() {
    local service="$1"
    
    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        exit 1
    fi
    
    log_warn "回滚 $service..."
    kubectl rollout undo deployment/$service -n langtou
    kubectl rollout status deployment/$service -n langtou --timeout=300s
    log_success "$service 回滚完成"
}

# 查看状态
status() {
    log_info "查看部署状态..."
    echo ""
    echo "=== Pods ==="
    kubectl get pods -n langtou
    echo ""
    echo "=== Services ==="
    kubectl get svc -n langtou
    echo ""
    echo "=== Ingress ==="
    kubectl get ingress -n langtou
    echo ""
    echo "=== HPA ==="
    kubectl get hpa -n langtou
}

# 查看日志
logs() {
    local service="$1"
    local tail="${2:-100}"
    
    if [ -z "$service" ]; then
        log_error "请指定服务名称"
        exit 1
    fi
    
    kubectl logs -f deployment/$service -n langtou --tail=$tail
}

# 主函数
main() {
    case "${1:-deploy}" in
        deploy)
            full_deploy
            ;;
        update)
            rolling_update "$2" "$3"
            ;;
        rollback)
            rollback "$2"
            ;;
        status)
            status
            ;;
        logs)
            logs "$2" "$3"
            ;;
        *)
            echo "用法: $0 [deploy|update|rollback|status|logs]"
            echo ""
            echo "部署命令:"
            echo "  deploy                    - 完整部署"
            echo ""
            echo "运维命令:"
            echo "  update <service> [tag]    - 滚动更新指定服务"
            echo "  rollback <service>        - 回滚指定服务"
            echo "  status                    - 查看部署状态"
            echo "  logs <service> [tail]     - 查看服务日志"
            echo ""
            echo "示例:"
            echo "  $0 deploy                          # 完整部署"
            echo "  $0 update gateway v1.2.3           # 更新gateway到v1.2.3"
            echo "  $0 rollback user-service           # 回滚user-service"
            echo "  $0 logs gateway 200                # 查看gateway最近200行日志"
            exit 1
            ;;
    esac
}

main "$@"
