#!/bin/bash
# ==========================================
# Langtou - 金丝雀部署自动化脚本
# 功能: 自动化金丝雀部署流程
# 用法: ./canary-deploy.sh <service> <canary-image> [--promote] [--rollback] [--set-weight <weight>]
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
NAMESPACE="langtou"
KUBECTL="kubectl"
TIMEOUT=300
CANARY_DEPLOY_DIR="/workspace/langtou-devops/k8s/canary"
LOG_FILE="/var/log/langtou/canary-deploy-$(date +%Y%m%d-%H%M%S).log"

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

    # 检查 kubectl
    if ! command -v kubectl &>/dev/null; then
        log_error "kubectl 未安装，请先安装 kubectl"
        exit 1
    fi

    # 检查集群连接
    if ! kubectl cluster-info &>/dev/null; then
        log_error "无法连接 Kubernetes 集群"
        exit 1
    fi

    # 检查命名空间
    if ! kubectl get namespace "${NAMESPACE}" &>/dev/null; then
        log_error "命名空间 ${NAMESPACE} 不存在"
        exit 1
    fi

    # 检查 Argo Rollouts
    if ! kubectl get crd rollouts.argoproj.io &>/dev/null; then
        log_warn "Argo Rollouts CRD 未安装，将使用原生 Ingress 金丝雀方案"
        USE_ARGO_ROLLOUTS=false
    else
        USE_ARGO_ROLLOUTS=true
    fi

    # 检查 Nginx Ingress Controller
    if ! kubectl get pods -n ingress-nginx &>/dev/null; then
        log_error "Nginx Ingress Controller 未安装"
        exit 1
    fi

    log_info "前置条件检查通过"
}

# ==========================================
# 部署金丝雀版本
# ==========================================
deploy_canary() {
    local service="$1"
    local canary_image="$2"

    log_step "开始部署金丝雀版本..."
    log_info "服务: ${service}"
    log_info "金丝雀镜像: ${canary_image}"

    # 1. 部署金丝雀 Deployment
    log_step "部署金丝雀 Deployment..."
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${service}-canary
  namespace: ${NAMESPACE}
  labels:
    app: ${service}
    track: canary
spec:
  replicas: 1
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: ${service}
      track: canary
  template:
    metadata:
      labels:
        app: ${service}
        track: canary
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
    spec:
      imagePullSecrets:
        - name: langtou-docker-registry
      containers:
        - name: ${service}
          image: ${canary_image}
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
          envFrom:
            - configMapRef:
                name: langtou-service-config
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"
            - name: TZ
              value: "Asia/Shanghai"
          resources:
            requests:
              memory: "256Mi"
              cpu: "125m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
            failureThreshold: 3
EOF

    # 2. 创建金丝雀 Service
    log_step "创建金丝雀 Service..."
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Service
metadata:
  name: ${service}-canary
  namespace: ${NAMESPACE}
  labels:
    app: ${service}
    track: canary
spec:
  type: ClusterIP
  selector:
    app: ${service}
    track: canary
  ports:
    - port: 8080
      targetPort: 8080
      name: http
EOF

    # 3. 创建金丝雀 Ingress (10%流量)
    log_step "创建金丝雀 Ingress (10%流量)..."
    cat <<EOF | kubectl apply -f -
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ${service}-canary-ingress
  namespace: ${NAMESPACE}
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/canary: "true"
    nginx.ingress.kubernetes.io/canary-weight: "10"
spec:
  tls:
    - hosts:
        - api.langtou.com
      secretName: langtou-tls
  rules:
    - host: api.langtou.com
      http:
        paths:
          - path: /api/${service}
            pathType: Prefix
            backend:
              service:
                name: ${service}-canary
                port:
                  number: 8080
EOF

    # 4. 等待金丝雀 Pod 就绪
    log_step "等待金丝雀 Pod 就绪..."
    kubectl rollout status deployment/${service}-canary \
        -n ${NAMESPACE} \
        --timeout=${TIMEOUT}s

    log_info "金丝雀版本部署完成，当前流量分配: 稳定版 90% / 金丝雀 10%"
}

# ==========================================
# 设置流量权重
# ==========================================
set_weight() {
    local service="$1"
    local weight="$2"

    log_step "设置金丝雀流量权重: ${weight}%"

    # 更新 Ingress 注解
    kubectl annotate ingress ${service}-canary-ingress \
        -n ${NAMESPACE} \
        nginx.ingress.kubernetes.io/canary-weight="${weight}" \
        --overwrite

    log_info "流量权重已更新: 稳定版 $((100 - weight))% / 金丝雀 ${weight}%"
}

# ==========================================
# 渐进式发布流程
# ==========================================
progressive_rollout() {
    local service="$1"

    log_step "开始渐进式金丝雀发布流程..."

    # 阶段1: 10% 流量
    log_info "===== 阶段1: 10% 流量 ====="
    set_weight "${service}" 10
    log_warn "请验证金丝雀版本是否正常 (等待5分钟)..."
    log_warn "确认无误后按 Enter 继续，输入 'abort' 中止发布"
    read -r -p "是否继续? (Enter继续 / abort中止): " confirm
    if [[ "${confirm}" == "abort" ]]; then
        log_error "发布已中止，执行回滚..."
        rollback "${service}"
        exit 1
    fi

    # 阶段2: 30% 流量
    log_info "===== 阶段2: 30% 流量 ====="
    set_weight "${service}" 30
    log_warn "请验证金丝雀版本是否正常 (等待5分钟)..."
    log_warn "确认无误后按 Enter 继续，输入 'abort' 中止发布"
    read -r -p "是否继续? (Enter继续 / abort中止): " confirm
    if [[ "${confirm}" == "abort" ]]; then
        log_error "发布已中止，执行回滚..."
        rollback "${service}"
        exit 1
    fi

    # 阶段3: 50% 流量
    log_info "===== 阶段3: 50% 流量 ====="
    set_weight "${service}" 50
    log_warn "请验证金丝雀版本是否正常 (等待10分钟)..."
    log_warn "确认无误后按 Enter 继续，输入 'abort' 中止发布"
    read -r -p "是否继续? (Enter继续 / abort中止): " confirm
    if [[ "${confirm}" == "abort" ]]; then
        log_error "发布已中止，执行回滚..."
        rollback "${service}"
        exit 1
    fi

    # 阶段4: 100% 全量发布
    log_info "===== 阶段4: 100% 全量发布 ====="
    promote "${service}"
}

# ==========================================
# 全量发布 (Promote)
# ==========================================
promote() {
    local service="$1"

    log_step "执行全量发布..."

    # 1. 更新稳定版 Deployment 镜像
    local canary_image
    canary_image=$(kubectl get deployment ${service}-canary \
        -n ${NAMESPACE} \
        -o jsonpath='{.spec.template.spec.containers[0].image}')

    log_info "将稳定版镜像更新为: ${canary_image}"
    kubectl set image deployment/${service} \
        ${service}=${canary_image} \
        -n ${NAMESPACE}

    # 2. 等待稳定版更新完成
    kubectl rollout status deployment/${service} \
        -n ${NAMESPACE} \
        --timeout=${TIMEOUT}s

    # 3. 删除金丝雀资源
    log_info "清理金丝雀资源..."
    kubectl delete ingress ${service}-canary-ingress -n ${NAMESPACE} --ignore-not-found
    kubectl delete service ${service}-canary -n ${NAMESPACE} --ignore-not-found
    kubectl delete deployment ${service}-canary -n ${NAMESPACE} --ignore-not-found

    log_info "全量发布完成!"
}

# ==========================================
# 回滚 (Rollback)
# ==========================================
rollback() {
    local service="$1"

    log_step "执行回滚操作..."

    # 1. 删除金丝雀 Ingress (立即停止金丝雀流量)
    log_info "删除金丝雀 Ingress..."
    kubectl delete ingress ${service}-canary-ingress -n ${NAMESPACE} --ignore-not-found

    # 2. 删除金丝雀 Service
    log_info "删除金丝雀 Service..."
    kubectl delete service ${service}-canary -n ${NAMESPACE} --ignore-not-found

    # 3. 删除金丝雀 Deployment
    log_info "删除金丝雀 Deployment..."
    kubectl delete deployment ${service}-canary -n ${NAMESPACE} --ignore-not-found

    # 4. 确认稳定版状态
    log_info "确认稳定版状态..."
    kubectl rollout status deployment/${service} -n ${NAMESPACE}

    log_info "回滚完成! 所有流量已恢复到稳定版本"
}

# ==========================================
# 查看金丝雀状态
# ==========================================
status() {
    local service="$1"

    echo ""
    echo "=========================================="
    echo "  金丝雀部署状态 - ${service}"
    echo "=========================================="

    # 稳定版状态
    echo ""
    echo "--- 稳定版 Deployment ---"
    kubectl get deployment ${service} -n ${NAMESPACE} -o wide 2>/dev/null || echo "未找到稳定版"

    # 金丝雀状态
    echo ""
    echo "--- 金丝雀 Deployment ---"
    kubectl get deployment ${service}-canary -n ${NAMESPACE} -o wide 2>/dev/null || echo "未找到金丝雀版本"

    # 金丝雀流量权重
    echo ""
    echo "--- 金丝雀流量权重 ---"
    local weight
    weight=$(kubectl get ingress ${service}-canary-ingress \
        -n ${NAMESPACE} \
        -o jsonpath='{.metadata.annotations.nginx\.ingress\.kubernetes\.io/canary-weight}' 2>/dev/null || echo "N/A")
    echo "金丝雀流量: ${weight}%"
    echo "稳定版流量: $((100 - weight))%"

    # Pod 状态
    echo ""
    echo "--- Pod 状态 ---"
    kubectl get pods -n ${NAMESPACE} -l "app=${service}" -o wide

    echo ""
    echo "=========================================="
}

# ==========================================
# 使用帮助
# ==========================================
usage() {
    echo "用法: $0 <command> [options]"
    echo ""
    echo "命令:"
    echo "  deploy <service> <canary-image>    部署金丝雀版本 (10%流量)"
    echo "  rollout <service>                   渐进式发布 (10%->30%->50%->100%)"
    echo "  promote <service>                   全量发布 (将金丝雀升级为稳定版)"
    echo "  rollback <service>                  回滚 (删除金丝雀，恢复全量到稳定版)"
    echo "  weight <service> <weight>           手动设置金丝雀流量权重 (0-100)"
    echo "  status <service>                    查看金丝雀部署状态"
    echo ""
    echo "示例:"
    echo "  $0 deploy user-service registry.langtou.local/langtou/user-service:v2.0.0-canary"
    echo "  $0 rollout user-service"
    echo "  $0 promote user-service"
    echo "  $0 rollback user-service"
    echo "  $0 weight user-service 30"
    echo "  $0 status user-service"
}

# ==========================================
# 主入口
# ==========================================
main() {
    # 创建日志目录
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/canary-deploy-$(date +%Y%m%d-%H%M%S).log"

    local command="${1:-}"
    shift || true

    case "${command}" in
        deploy)
            [[ $# -lt 2 ]] && { log_error "缺少参数: service canary-image"; usage; exit 1; }
            check_prerequisites
            deploy_canary "$1" "$2"
            ;;
        rollout)
            [[ $# -lt 1 ]] && { log_error "缺少参数: service"; usage; exit 1; }
            check_prerequisites
            progressive_rollout "$1"
            ;;
        promote)
            [[ $# -lt 1 ]] && { log_error "缺少参数: service"; usage; exit 1; }
            check_prerequisites
            promote "$1"
            ;;
        rollback)
            [[ $# -lt 1 ]] && { log_error "缺少参数: service"; usage; exit 1; }
            check_prerequisites
            rollback "$1"
            ;;
        weight)
            [[ $# -lt 2 ]] && { log_error "缺少参数: service weight"; usage; exit 1; }
            check_prerequisites
            set_weight "$1" "$2"
            ;;
        status)
            [[ $# -lt 1 ]] && { log_error "缺少参数: service"; usage; exit 1; }
            status "$1"
            ;;
        -h|--help|help)
            usage
            ;;
        *)
            log_error "未知命令: ${command}"
            usage
            exit 1
            ;;
    esac
}

main "$@"
