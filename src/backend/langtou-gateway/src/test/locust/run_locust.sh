#!/usr/bin/env bash
# =============================================================================
# Langtou (榔头) Gateway - Locust 性能测试启动脚本
# =============================================================================
# 用法:
#   单机模式 : ./run_locust.sh standalone
#   主节点   : ./run_locust.sh master
#   从节点   : ./run_locust.sh worker http://master-host:5557
#
# 示例:
#   ./run_locust.sh standalone
#   ./run_locust.sh master
#   ./run_locust.sh worker http://192.168.1.100:5557
# =============================================================================

set -euo pipefail

# -----------------------------------------------------------------------------
# 配置区
# -----------------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCUST_FILE="${SCRIPT_DIR}/performance_test.py"
HOST="http://localhost:8080"
MASTER_BIND_HOST="0.0.0.0"
MASTER_BIND_PORT="5557"

# 单机模式默认参数
STANDALONE_USERS="1000"
STANDALONE_SPAWN_RATE="100"
STANDALONE_RUN_TIME="10m"

# 报告输出目录
REPORT_DIR="${SCRIPT_DIR}/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# -----------------------------------------------------------------------------
# 工具函数
# -----------------------------------------------------------------------------
log_info() {
    echo -e "\033[32m[INFO]\033[0m  $1"
}

log_warn() {
    echo -e "\033[33m[WARN]\033[0m  $1"
}

log_error() {
    echo -e "\033[31m[ERROR]\033[0m $1"
}

check_locust() {
    if ! command -v locust &>/dev/null; then
        log_error "未检测到 locust 命令，请先安装 Locust:"
        log_error "  pip3 install locust>=1.6.0"
        exit 1
    fi

    local locust_version
    locust_version=$(locust --version | head -n1 | grep -oP '\d+\.\d+\.\d+' | head -n1)
    log_info "Locust 版本: ${locust_version}"
}

check_locustfile() {
    if [[ ! -f "${LOCUST_FILE}" ]]; then
        log_error "Locust 脚本不存在: ${LOCUST_FILE}"
        exit 1
    fi
}

mkdir_report_dir() {
    if [[ ! -d "${REPORT_DIR}" ]]; then
        mkdir -p "${REPORT_DIR}"
        log_info "创建报告目录: ${REPORT_DIR}"
    fi
}

print_usage() {
    cat <<EOF
用法:
  $(basename "$0") <mode> [master-url]

模式:
  standalone   单机模式（Web UI + 自动运行）
  master       分布式主节点模式
  worker       分布式从节点模式（需指定 master-url）

示例:
  $(basename "$0") standalone
  $(basename "$0") master
  $(basename "$0") worker http://192.168.1.100:5557

环境变量:
  LOCUST_HOST          目标服务地址（默认: ${HOST}）
  LOCUST_USERS         单机模式总用户数（默认: ${STANDALONE_USERS}）
  LOCUST_SPAWN_RATE    单机模式每秒启动用户数（默认: ${STANDALONE_SPAWN_RATE}）
  LOCUST_RUN_TIME      单机模式运行时长（默认: ${STANDALONE_RUN_TIME}）
  LOCUST_MASTER_PORT   Master 绑定端口（默认: ${MASTER_BIND_PORT}）
EOF
}

# -----------------------------------------------------------------------------
# 运行模式函数
# -----------------------------------------------------------------------------

run_standalone() {
    local users=${LOCUST_USERS:-${STANDALONE_USERS}}
    local spawn_rate=${LOCUST_SPAWN_RATE:-${STANDALONE_SPAWN_RATE}}
    local run_time=${LOCUST_RUN_TIME:-${STANDALONE_RUN_TIME}}
    local report_file="${REPORT_DIR}/report_standalone_${TIMESTAMP}.html"
    local csv_prefix="${REPORT_DIR}/csv_standalone_${TIMESTAMP}"

    log_info "启动 Locust 单机模式"
    log_info "  目标服务 : ${HOST}"
    log_info "  总用户数 : ${users}"
    log_info "  启动速率 : ${spawn_rate}/s"
    log_info "  运行时长 : ${run_time}"
    log_info "  HTML报告 : ${report_file}"
    log_info "  CSV前缀  : ${csv_prefix}"

    locust -f "${LOCUST_FILE}" \
        --host="${HOST}" \
        --users="${users}" \
        --spawn-rate="${spawn_rate}" \
        --run-time="${run_time}" \
        --html="${report_file}" \
        --csv="${csv_prefix}" \
        --auto-start \
        --headless \
        --print-stats \
        --only-summary

    log_info "测试完成，报告已生成:"
    log_info "  HTML 报告: ${report_file}"
    log_info "  CSV 统计 : ${csv_prefix}_stats.csv"
    log_info "  失败详情 : ${csv_prefix}_failures.csv"
}

run_master() {
    local report_file="${REPORT_DIR}/report_master_${TIMESTAMP}.html"
    local csv_prefix="${REPORT_DIR}/csv_master_${TIMESTAMP}"

    log_info "启动 Locust 分布式主节点 (Master)"
    log_info "  目标服务   : ${HOST}"
    log_info "  绑定地址   : ${MASTER_BIND_HOST}:${MASTER_BIND_PORT}"
    log_info "  HTML报告   : ${report_file}"
    log_info "  CSV前缀    : ${csv_prefix}"
    log_info ""
    log_info "等待 Worker 连接中... (按 Ctrl+C 停止)"
    log_info "Worker 启动命令示例:"
    log_info "  ./run_locust.sh worker http://<MASTER_IP>:${MASTER_BIND_PORT}"

    locust -f "${LOCUST_FILE}" \
        --master \
        --master-bind-host="${MASTER_BIND_HOST}" \
        --master-bind-port="${MASTER_BIND_PORT}" \
        --host="${HOST}" \
        --html="${report_file}" \
        --csv="${csv_prefix}" \
        --print-stats \
        --only-summary

    log_info "Master 测试完成，报告已生成:"
    log_info "  HTML 报告: ${report_file}"
}

run_worker() {
    local master_url="${1:-}"

    if [[ -z "${master_url}" ]]; then
        log_error "Worker 模式需要指定 Master 地址"
        log_error "  示例: ./run_locust.sh worker http://192.168.1.100:5557"
        exit 1
    fi

    # 解析 master host 和 port
    local master_host master_port
    if [[ "${master_url}" =~ ^http://([^:/]+)(:([0-9]+))?/?$ ]]; then
        master_host="${BASH_REMATCH[1]}"
        master_port="${BASH_REMATCH[3]:-${MASTER_BIND_PORT}}"
    else
        log_error "Master 地址格式错误: ${master_url}"
        log_error "  正确格式: http://host:port"
        exit 1
    fi

    log_info "启动 Locust 分布式从节点 (Worker)"
    log_info "  目标服务   : ${HOST}"
    log_info "  Master节点 : ${master_host}:${master_port}"

    locust -f "${LOCUST_FILE}" \
        --worker \
        --master-host="${master_host}" \
        --master-port="${master_port}" \
        --host="${HOST}"

    log_info "Worker 已断开与 Master 的连接"
}

# -----------------------------------------------------------------------------
# 主逻辑
# -----------------------------------------------------------------------------

main() {
    if [[ $# -lt 1 ]]; then
        print_usage
        exit 1
    fi

    local mode="${1}"
    shift

    # 允许通过环境变量覆盖 HOST
    if [[ -n "${LOCUST_HOST:-}" ]]; then
        HOST="${LOCUST_HOST}"
    fi

    if [[ -n "${LOCUST_MASTER_PORT:-}" ]]; then
        MASTER_BIND_PORT="${LOCUST_MASTER_PORT}"
    fi

    check_locust
    check_locustfile
    mkdir_report_dir

    case "${mode}" in
        standalone)
            run_standalone
            ;;
        master)
            run_master
            ;;
        worker)
            run_worker "$@"
            ;;
        help|--help|-h)
            print_usage
            exit 0
            ;;
        *)
            log_error "未知模式: ${mode}"
            print_usage
            exit 1
            ;;
    esac
}

main "$@"
