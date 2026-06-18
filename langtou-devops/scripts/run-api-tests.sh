#!/bin/bash
# ==========================================
# Langtou API 集成测试运行脚本
# ==========================================
# 功能:
#   1. 启动 docker-compose 测试环境
#   2. 等待所有服务健康检查通过
#   3. 运行全部 REST Assured 集成测试
#   4. 生成测试报告
#   5. 返回测试结果退出码
# ==========================================
# 用法:
#   ./scripts/run-api-tests.sh [--skip-env] [--skip-cleanup]
#   --skip-env     : 跳过环境启动/停止 (用于环境已运行的情况)
#   --skip-cleanup : 测试完成后不清理环境
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/../langtou-backend"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.dev.yml"
REPORT_DIR="${BACKEND_DIR}/test-reports"

# 服务健康检查配置
HEALTH_CHECK_TIMEOUT=300    # 最大等待时间(秒)
HEALTH_CHECK_INTERVAL=10    # 检查间隔(秒)
HEALTH_CHECK_RETRIES=$((HEALTH_CHECK_TIMEOUT / HEALTH_CHECK_INTERVAL))

# 需要健康检查的服务列表 (服务名:健康检查URL:端口)
SERVICES=(
    "langtou-mysql-dev:mysql:3306"
    "langtou-redis-dev:redis:6379"
    "langtou-elasticsearch-dev:elasticsearch:9200"
    "langtou-nacos-dev:nacos:8848"
    "langtou-user-service-dev:user-service:8080"
    "langtou-content-service-dev:content-service:8080"
    "langtou-interact-service-dev:interact-service:8080"
    "langtou-message-service-dev:message-service:8080"
    "langtou-gateway-dev:gateway:8080"
)

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 命令行参数
SKIP_ENV=false
SKIP_CLEANUP=false

# ==========================================
# 解析命令行参数
# ==========================================
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-env)
            SKIP_ENV=true
            shift
            ;;
        --skip-cleanup)
            SKIP_CLEANUP=true
            shift
            ;;
        *)
            echo -e "${RED}未知参数: $1${NC}"
            echo "用法: $0 [--skip-env] [--skip-cleanup]"
            exit 1
            ;;
    esac
done

# ==========================================
# 工具函数
# ==========================================

log_info() {
    echo -e "${BLUE}[INFO]$(date '+%Y-%m-%d %H:%M:%S') $1${NC}"
}

log_success() {
    echo -e "${GREEN}[PASS]$(date '+%Y-%m-%d %H:%M:%S') $1${NC}"
}

log_warn() {
    echo -e "${YELLOW}[WARN]$(date '+%Y-%m-%d %H:%M:%S') $1${NC}"
}

log_error() {
    echo -e "${RED}[FAIL]$(date '+%Y-%m-%d %H:%M:%S') $1${NC}"
}

# ==========================================
# 清理函数 (确保环境被正确关闭)
# ==========================================
cleanup() {
    local exit_code=$?
    if [[ "${SKIP_CLEANUP}" == "false" && "${SKIP_ENV}" == "false" ]]; then
        log_info "正在停止测试环境..."
        cd "${PROJECT_ROOT}"
        docker compose -f "${COMPOSE_FILE}" --env-file .env down --volumes --remove-orphans 2>/dev/null || true
        log_info "测试环境已停止"
    fi
    exit ${exit_code}
}

trap cleanup EXIT INT TERM

# ==========================================
# Step 1: 启动 Docker Compose 测试环境
# ==========================================
start_test_environment() {
    log_info "=========================================="
    log_info "Step 1: 启动 Docker Compose 测试环境..."
    log_info "=========================================="

    cd "${PROJECT_ROOT}"

    # 检查 .env 文件是否存在
    if [[ ! -f ".env" ]]; then
        log_warn ".env 文件不存在，尝试使用默认配置..."
        # 创建最小化 .env 文件用于测试
        cat > .env.test <<'EOF'
# Langtou 测试环境配置
MYSQL_ROOT_PASSWORD=test_root_pass_123
MYSQL_DATABASE=langtou_test
MYSQL_USER=langtou_test
MYSQL_PASSWORD=test_pass_123
REDIS_PASSWORD=test_redis_pass_123
ELASTIC_PASSWORD=test_es_pass_123
TZ=Asia/Shanghai
KAFKA_BROKER_ID=1
KAFKA_ZOOKEEPER_CONNECT=langtou-zookeeper-dev:2181
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://langtou-kafka-dev:9092
GATEWAY_PORT=8080
USER_SERVICE_PORT=8081
CONTENT_SERVICE_PORT=8082
INTERACT_SERVICE_PORT=8083
MESSAGE_SERVICE_PORT=8084
RECOMMENDATION_PORT=8000
DOCKER_REGISTRY=registry.langtou.local
DOCKER_NAMESPACE=langtou
IMAGE_TAG=latest
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
EOF
        log_warn "已创建 .env.test 文件，请确保配置正确"
        cp .env.test .env
    fi

    # 停止可能存在的旧容器
    log_info "清理旧容器..."
    docker compose -f "${COMPOSE_FILE}" --env-file .env down --volumes --remove-orphans 2>/dev/null || true

    # 启动基础设施服务 (MySQL, Redis, ES, Nacos, Kafka等)
    log_info "启动基础设施服务..."
    docker compose -f "${COMPOSE_FILE}" --env-file .env up -d \
        langtou-mysql-dev \
        langtou-redis-dev \
        langtou-elasticsearch-dev \
        langtou-nacos-dev \
        langtou-zookeeper-dev \
        langtou-kafka-dev \
        langtou-minio-dev

    log_info "等待基础设施服务启动..."
    sleep 15

    # 启动后端业务服务
    log_info "启动后端业务服务..."
    docker compose -f "${COMPOSE_FILE}" --env-file .env up -d \
        langtou-user-service-dev \
        langtou-content-service-dev \
        langtou-interact-service-dev \
        langtou-message-service-dev \
        langtou-gateway-dev

    log_success "Docker Compose 测试环境启动命令已执行"
}

# ==========================================
# Step 2: 等待所有服务健康检查通过
# ==========================================
wait_for_services_healthy() {
    log_info "=========================================="
    log_info "Step 2: 等待所有服务健康检查通过..."
    log_info "=========================================="

    local all_healthy=false
    local attempt=0

    while [[ ${attempt} -lt ${HEALTH_CHECK_RETRIES} && "${all_healthy}" == "false" ]]; do
        attempt=$((attempt + 1))
        all_healthy=true
        local failed_services=""

        for service_info in "${SERVICES[@]}"; do
            IFS=':' read -r service_name service_type service_port <<< "${service_info}"

            # 检查容器是否运行
            local container_status
            container_status=$(docker inspect --format='{{.State.Status}}' "${service_name}" 2>/dev/null || echo "not_found")

            if [[ "${container_status}" != "running" ]]; then
                all_healthy=false
                failed_services="${failed_services}  - ${service_name} (状态: ${container_status})\n"
                continue
            fi

            # 检查容器健康状态
            local health_status
            health_status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${service_name}" 2>/dev/null || echo "none")

            if [[ "${health_status}" != "healthy" ]]; then
                all_healthy=false
                failed_services="${failed_services}  - ${service_name} (健康: ${health_status})\n"
            fi
        done

        if [[ "${all_healthy}" == "true" ]]; then
            log_success "所有服务健康检查通过! (尝试 ${attempt}/${HEALTH_CHECK_RETRIES})"
            return 0
        else
            if [[ ${attempt} -lt ${HEALTH_CHECK_RETRIES} ]]; then
                log_warn "部分服务尚未就绪 (${attempt}/${HEALTH_CHECK_RETRIES})，${HEALTH_CHECK_INTERVAL}秒后重试..."
                log_warn "未就绪服务:\n${failed_services}"
                sleep ${HEALTH_CHECK_INTERVAL}
            fi
        fi
    done

    log_error "服务健康检查超时! 以下服务未能在 ${HEALTH_CHECK_TIMEOUT} 秒内就绪:"
    echo -e "${failed_services}"

    # 输出诊断信息
    log_info "===== 诊断信息 ====="
    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r service_name service_type service_port <<< "${service_info}"
        log_info "--- ${service_name} ---"
        docker logs --tail 20 "${service_name}" 2>/dev/null || echo "  (无日志)"
    done

    return 1
}

# ==========================================
# Step 3: 运行 REST Assured 集成测试
# ==========================================
run_api_tests() {
    log_info "=========================================="
    log_info "Step 3: 运行 REST Assured API 集成测试..."
    log_info "=========================================="

    # 创建测试报告目录
    mkdir -p "${REPORT_DIR}"

    # 设置测试环境变量
    export TEST_ENV=docker
    export GATEWAY_BASE_URL="http://localhost:8080"
    export USER_SERVICE_URL="http://localhost:8081"
    export CONTENT_SERVICE_URL="http://localhost:8082"
    export INTERACT_SERVICE_URL="http://localhost:8083"
    export MESSAGE_SERVICE_URL="http://localhost:8084"

    cd "${BACKEND_DIR}"

    # 使用 mvn verify 跳过单元测试，只运行集成测试 (IT)
    log_info "执行: mvn verify -DskipUnitTests -DskipTests=false ..."
    if mvn verify \
        -DskipUnitTests \
        -DskipTests=false \
        -Dmaven.test.failure.ignore=false \
        -Dsurefire.useFile=false \
        -Dfailsafe.useFile=false \
        -Dtest.report.dir="${REPORT_DIR}" \
        --batch-mode 2>&1 | tee "${REPORT_DIR}/api-test-output.log"; then
        log_success "API 集成测试全部通过!"
        return 0
    else
        local test_exit_code=$?
        log_error "API 集成测试失败! (退出码: ${test_exit_code})"
        return ${test_exit_code}
    fi
}

# ==========================================
# Step 4: 生成测试报告
# ==========================================
generate_test_report() {
    log_info "=========================================="
    log_info "Step 4: 生成测试报告..."
    log_info "=========================================="

    cd "${BACKEND_DIR}"

    # 汇总测试结果
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    local skipped_tests=0
    local report_summary=""

    # 解析 Failsafe 报告 (集成测试)
    for xml_report in $(find . -path '*/target/failsafe-reports/TEST-*.xml' 2>/dev/null); do
        if [[ -f "${xml_report}" ]]; then
            local tests=$(grep -oP 'tests="\K[^"]+' "${xml_report}" 2>/dev/null || echo "0")
            local failures=$(grep -oP 'failures="\K[^"]+' "${xml_report}" 2>/dev/null || echo "0")
            local errors=$(grep -oP 'errors="\K[^"]+' "${xml_report}" 2>/dev/null || echo "0")
            local skipped=$(grep -oP 'skipped="\K[^"]+' "${xml_report}" 2>/dev/null || echo "0")

            tests=${tests:-0}
            failures=${failures:-0}
            errors=${errors:-0}
            skipped=${skipped:-0}

            total_tests=$((total_tests + tests))
            failed_tests=$((failed_tests + failures + errors))
            skipped_tests=$((skipped_tests + skipped))
            passed_tests=$((total_tests - failed_tests - skipped_tests))

            local test_class=$(basename "${xml_report}" .xml)
            report_summary="${report_summary}  ${test_class}: ${tests} tests, ${failures} failures, ${errors} errors, ${skipped} skipped\n"
        fi
    done

    # 生成汇总报告
    cat > "${REPORT_DIR}/api-test-summary.txt" <<EOF
==========================================
Langtou API 集成测试报告汇总
==========================================
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
测试环境: Docker Compose (docker-compose.dev.yml)
------------------------------------------
总测试数:   ${total_tests}
通过:       ${passed_tests}
失败:       ${failed_tests}
跳过:       ${skipped_tests}
通过率:     $(awk "BEGIN {if (${total_tests} > 0) printf \"%.1f%%\", ${passed_tests}/${total_tests}*100; else print \"N/A\"}")
------------------------------------------
测试详情:
${report_summary}
==========================================
EOF

    log_info "测试报告已生成:"
    log_info "  - 汇总: ${REPORT_DIR}/api-test-summary.txt"
    log_info "  - 日志: ${REPORT_DIR}/api-test-output.log"

    # 输出汇总
    cat "${REPORT_DIR}/api-test-summary.txt"
}

# ==========================================
# 主流程
# ==========================================
main() {
    log_info "=========================================="
    log_info "Langtou API 集成测试流水线"
    log_info "开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
    log_info "=========================================="

    # Step 1: 启动测试环境
    if [[ "${SKIP_ENV}" == "false" ]]; then
        start_test_environment
    else
        log_warn "跳过环境启动 (使用已有环境)"
    fi

    # Step 2: 等待服务健康
    if [[ "${SKIP_ENV}" == "false" ]]; then
        if ! wait_for_services_healthy; then
            log_error "服务健康检查失败，终止测试流程"
            exit 1
        fi
    else
        log_warn "跳过服务健康检查 (使用已有环境)"
    fi

    # Step 3: 运行API测试
    local test_result=0
    if ! run_api_tests; then
        test_result=$?
    fi

    # Step 4: 生成报告 (无论测试是否通过)
    generate_test_report

    # 输出最终结果
    log_info "=========================================="
    if [[ ${test_result} -eq 0 ]]; then
        log_success "API 集成测试全部通过!"
        log_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
        log_info "=========================================="
        exit 0
    else
        log_error "API 集成测试失败! (退出码: ${test_result})"
        log_info "结束时间: $(date '+%Y-%m-%d %H:%M:%S')"
        log_info "=========================================="
        exit ${test_result}
    fi
}

# 执行主流程
main "$@"
