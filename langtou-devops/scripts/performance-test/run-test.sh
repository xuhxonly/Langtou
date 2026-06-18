#!/bin/bash
# ============================================================
# 榔头(Langtou)性能压测启动脚本
# 使用Locust框架进行HTTP性能压测
# ============================================================

set -e

# ============================================================
# 配置参数（可通过环境变量覆盖）
# ============================================================
HOST="${LOCUST_HOST:-http://localhost:8080}"
USERS="${LOCUST_USERS:-100}"
SPAWN_RATE="${LOCUST_SPAWN_RATE:-10}"
RUN_TIME="${LOCUST_RUN_TIME:-300}"
LOCUST_FILE="${LOCUST_FILE:-locustfile.py}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}  榔头(Langtou)性能压测${NC}"
echo -e "${BLUE}============================================================${NC}"
echo ""

# 检查Locust是否安装
if ! command -v locust &> /dev/null; then
    echo -e "${RED}错误: Locust未安装${NC}"
    echo "请先安装Locust: pip install locust"
    exit 1
fi

echo -e "${GREEN}Locust版本:${NC}"
locust --version
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCUST_FILE_PATH="${SCRIPT_DIR}/${LOCUST_FILE}"

# 检查locustfile是否存在
if [ ! -f "${LOCUST_FILE_PATH}" ]; then
    echo -e "${RED}错误: 压测脚本不存在: ${LOCUST_FILE_PATH}${NC}"
    exit 1
fi

echo -e "${GREEN}压测配置:${NC}"
echo "  目标服务:   ${HOST}"
echo "  并发用户数: ${USERS}"
echo "  启动速率:   ${SPAWN_RATE} 用户/秒"
echo "  持续时间:   ${RUN_TIME} 秒"
echo "  压测脚本:   ${LOCUST_FILE_PATH}"
echo ""

# 解析运行模式
MODE="${1:-web}"

case "${MODE}" in
    web)
        echo -e "${YELLOW}启动Web UI模式...${NC}"
        echo "  请在浏览器中打开 http://localhost:8089 进行压测配置"
        echo ""
        locust -f "${LOCUST_FILE_PATH}" --host="${HOST}" --port=8089
        ;;
    headless)
        echo -e "${YELLOW}启动无头模式（自动压测）...${NC}"
        echo "  压测将在 ${RUN_TIME} 秒后自动结束"
        echo ""
        locust -f "${LOCUST_FILE_PATH}" \
            --host="${HOST}" \
            --users="${USERS}" \
            --spawn-rate="${SPAWN_RATE}" \
            --run-time="${RUN_TIME}s" \
            --headless \
            --only-summary \
            --csv="${SCRIPT_DIR}/langtou_perf_results"
        echo ""
        echo -e "${GREEN}压测完成！结果已保存到:${NC}"
        echo "  ${SCRIPT_DIR}/langtou_perf_results_stats.csv"
        echo "  ${SCRIPT_DIR}/langtou_perf_results_failures.csv"
        echo "  ${SCRIPT_DIR}/langtou_perf_results_requests.csv"
        ;;
    distributed-master)
        echo -e "${YELLOW}启动分布式Master模式...${NC}"
        locust -f "${LOCUST_FILE_PATH}" \
            --host="${HOST}" \
            --master \
            --master-bind-port=5557 \
            --expect-workers="${2:-4}"
        ;;
    distributed-worker)
        echo -e "${YELLOW}启动分布式Worker模式...${NC}"
        WORKER_COUNT="${2:-1}"
        for i in $(seq 1 "${WORKER_COUNT}"); do
            echo "  启动Worker ${i}..."
            locust -f "${LOCUST_FILE_PATH}" \
                --host="${HOST}" \
                --worker \
                --master-host="${3:-localhost}" &
        done
        wait
        ;;
    *)
        echo -e "${RED}未知模式: ${MODE}${NC}"
        echo ""
        echo "使用方式: $0 <mode> [options]"
        echo ""
        echo "可用模式:"
        echo "  web                  Web UI模式（默认，浏览器控制）"
        echo "  headless             无头模式（自动运行压测）"
        echo "  distributed-master   分布式Master节点"
        echo "  distributed-worker   分布式Worker节点"
        echo ""
        echo "环境变量:"
        echo "  LOCUST_HOST          目标服务地址 (默认: http://localhost:8080)"
        echo "  LOCUST_USERS         并发用户数 (默认: 100)"
        echo "  LOCUST_SPAWN_RATE    启动速率 (默认: 10)"
        echo "  LOCUST_RUN_TIME      持续时间秒数 (默认: 300)"
        echo "  LOCUST_FILE          压测脚本文件 (默认: locustfile.py)"
        echo ""
        echo "示例:"
        echo "  $0 web                           # Web UI模式"
        echo "  $0 headless                      # 无头模式，100用户，5分钟"
        echo "  LOCUST_USERS=500 $0 headless     # 500用户无头模式"
        echo "  $0 distributed-master 4          # Master模式，4个Worker"
        echo "  $0 distributed-worker 2          # 启动2个Worker"
        exit 1
        ;;
esac
