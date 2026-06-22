#!/bin/bash
# ==========================================
# Langtou 测试覆盖率检查脚本
# ==========================================
# 功能:
#   1. 解析各服务 JaCoCo 覆盖率报告
#   2. 汇总整体覆盖率
#   3. 与阈值比较，未达标则阻断合并
#   4. 输出详细覆盖率报告
# ==========================================
# 用法:
#   ./scripts/check-coverage.sh [阈值百分比]
#   默认阈值: 75
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/../langtou-backend"
REPORT_DIR="${BACKEND_DIR}/test-reports"

# 默认覆盖率阈值
COVERAGE_THRESHOLD="${1:-75}"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 服务模块列表
MODULES=(
    "langtou-gateway"
    "langtou-user-service"
    "langtou-content-service"
    "langtou-interact-service"
    "langtou-message-service"
)

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
# 从 JaCoCo CSV 报告解析覆盖率
# ==========================================
parse_jacoco_csv() {
    local csv_file="$1"
    local metric="$2"  # INSTRUCTION, LINE, BRANCH, METHOD, CLASS

    if [[ ! -f "${csv_file}" ]]; then
        echo "0"
        return
    fi

    # JaCoCo CSV 格式: GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,...
    # 查找对应列索引
    local header_line
    header_line=$(head -1 "${csv_file}")

    local missed_col=-1
    local covered_col=-1

    case "${metric}" in
        INSTRUCTION)
            missed_col=3
            covered_col=4
            ;;
        LINE)
            missed_col=5
            covered_col=6
            ;;
        BRANCH)
            missed_col=7
            covered_col=8
            ;;
        METHOD)
            missed_col=9
            covered_col=10
            ;;
        CLASS)
            missed_col=11
            covered_col=12
            ;;
        *)
            echo "0"
            return
            ;;
    esac

    # 计算总覆盖率 (跳过表头)
    local total_missed=0
    local total_covered=0

    while IFS=',' read -r line; do
        # 跳过表头
        if [[ "${line}" == "GROUP"* ]]; then
            continue
        fi

        local fields
        IFS=',' read -ra fields <<< "${line}"

        local missed="${fields[${missed_col}]:-0}"
        local covered="${fields[${covered_col}]:-0}"

        # 确保是数字
        missed=$(echo "${missed}" | tr -d '[:space:]' | grep -oP '\d+' || echo "0")
        covered=$(echo "${covered}" | tr -d '[:space:]' | grep -oP '\d+' || echo "0")

        total_missed=$((total_missed + missed))
        total_covered=$((total_covered + covered))
    done < "${csv_file}"

    local total=$((total_missed + total_covered))
    if [[ ${total} -eq 0 ]]; then
        echo "0"
    else
        # 返回百分比 (整数)
        echo $((covered * 100 / total))
    fi
}

# ==========================================
# 检查单个模块覆盖率
# ==========================================
check_module_coverage() {
    local module="$1"
    local module_dir="${BACKEND_DIR}/${module}"
    local csv_report="${module_dir}/target/site/jacoco/jacoco.csv"

    if [[ ! -f "${csv_report}" ]]; then
        log_warn "模块 ${module} 未找到 JaCoCo 报告: ${csv_report}"
        echo "INSTRUCTION=0 LINE=0 BRANCH=0 METHOD=0"
        return
    fi

    local instruction_cov
    local line_cov
    local branch_cov
    local method_cov

    instruction_cov=$(parse_jacoco_csv "${csv_report}" "INSTRUCTION")
    line_cov=$(parse_jacoco_csv "${csv_report}" "LINE")
    branch_cov=$(parse_jacoco_csv "${csv_report}" "BRANCH")
    method_cov=$(parse_jacoco_csv "${csv_report}" "METHOD")

    echo "INSTRUCTION=${instruction_cov} LINE=${line_cov} BRANCH=${branch_cov} METHOD=${method_cov}"
}

# ==========================================
# 主流程
# ==========================================
main() {
    log_info "=========================================="
    log_info "Langtou 测试覆盖率检查"
    log_info "覆盖率门禁阈值: ${COVERAGE_THRESHOLD}%"
    log_info "=========================================="

    mkdir -p "${REPORT_DIR}"

    local total_instruction=0
    local total_line=0
    local total_branch=0
    local total_method=0
    local module_count=0
    local failed_modules=""

    # 检查每个模块的覆盖率
    for module in "${MODULES[@]}"; do
        log_info "检查模块: ${module}"

        local coverage_data
        coverage_data=$(check_module_coverage "${module}")

        local instruction_cov line_cov branch_cov method_cov
        instruction_cov=$(echo "${coverage_data}" | grep -oP 'INSTRUCTION=\K\d+')
        line_cov=$(echo "${coverage_data}" | grep -oP 'LINE=\K\d+')
        branch_cov=$(echo "${coverage_data}" | grep -oP 'BRANCH=\K\d+')
        method_cov=$(echo "${coverage_data}" | grep -oP 'METHOD=\K\d+')

        log_info "  指令覆盖: ${instruction_cov}% | 行覆盖: ${line_cov}% | 分支覆盖: ${branch_cov}% | 方法覆盖: ${method_cov}%"

        # 使用指令覆盖率作为主要指标
        if [[ ${instruction_cov} -lt ${COVERAGE_THRESHOLD} ]]; then
            log_error "  模块 ${module} 指令覆盖率 ${instruction_cov}% < ${COVERAGE_THRESHOLD}% (未达标)"
            failed_modules="${failed_modules}  - ${module}: ${instruction_cov}% (指令), ${line_cov}% (行), ${branch_cov}% (分支)\n"
        else
            log_success "  模块 ${module} 覆盖率达标"
        fi

        total_instruction=$((total_instruction + instruction_cov))
        total_line=$((total_line + line_cov))
        total_branch=$((total_branch + branch_cov))
        total_method=$((total_method + method_cov))
        module_count=$((module_count + 1))
    done

    # 计算平均覆盖率
    local avg_instruction=0
    local avg_line=0
    local avg_branch=0
    local avg_method=0

    if [[ ${module_count} -gt 0 ]]; then
        avg_instruction=$((total_instruction / module_count))
        avg_line=$((total_line / module_count))
        avg_branch=$((total_branch / module_count))
        avg_method=$((total_method / module_count))
    fi

    # 生成覆盖率报告
    cat > "${REPORT_DIR}/coverage-summary.txt" <<EOF
==========================================
Langtou 测试覆盖率检查报告
==========================================
检查时间: $(date '+%Y-%m-%d %H:%M:%S')
覆盖率门禁阈值: ${COVERAGE_THRESHOLD}%
------------------------------------------
整体平均覆盖率:
  指令覆盖 (Instruction): ${avg_instruction}%
  行覆盖 (Line):           ${avg_line}%
  分支覆盖 (Branch):       ${avg_branch}%
  方法覆盖 (Method):       ${avg_method}%
------------------------------------------
检查模块数: ${module_count}
------------------------------------------
未达标模块:
$(if [[ -n "${failed_modules}" ]]; then echo -e "${failed_modules}"; else echo "  (全部达标)"; fi)
==========================================
结论: $(if [[ ${avg_instruction} -ge ${COVERAGE_THRESHOLD} ]]; then echo "PASS - 覆盖率达到门禁要求"; else echo "FAIL - 覆盖率未达到门禁要求"; fi)
==========================================
EOF

    log_info "=========================================="
    log_info "覆盖率汇总:"
    log_info "  平均指令覆盖率: ${avg_instruction}% (阈值: ${COVERAGE_THRESHOLD}%)"
    log_info "  平均行覆盖率:   ${avg_line}%"
    log_info "  平均分支覆盖率: ${avg_branch}%"
    log_info "  平均方法覆盖率: ${avg_method}%"
    log_info "=========================================="

    # 输出报告文件
    cat "${REPORT_DIR}/coverage-summary.txt"

    # 判断是否通过门禁
    if [[ ${avg_instruction} -ge ${COVERAGE_THRESHOLD} ]]; then
        log_success "覆盖率门禁检查通过! (${avg_instruction}% >= ${COVERAGE_THRESHOLD}%)"
        exit 0
    else
        log_error "覆盖率门禁检查失败! (${avg_instruction}% < ${COVERAGE_THRESHOLD}%)"
        log_error "合并将被阻断，请提升测试覆盖率后重新提交。"
        log_error "未达标模块:"
        echo -e "${failed_modules}"
        exit 1
    fi
}

# 执行主流程
main "$@"
