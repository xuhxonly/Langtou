#!/bin/bash
# ==========================================
# Langtou - 安全扫描脚本
# 功能: 使用trivy扫描依赖漏洞，使用bandit扫描Python代码安全
# 用法: ./security-scan.sh [--full] [--fix] [--report <output-dir>]
# ==========================================

set -euo pipefail

# ==========================================
# 配置变量
# ==========================================
PROJECT_DIR="/workspace"
REPORT_DIR="${PROJECT_DIR}/security-reports"
TRIVY_SEVERITY="CRITICAL,HIGH"
TRIVY_FORMAT="table"
SCAN_DATE=$(date +%Y%m%d-%H%M%S)
LOG_FILE="/var/log/langtou/security-scan-${SCAN_DATE}.log"

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

# 扫描结果统计
VULN_CRITICAL=0
VULN_HIGH=0
VULN_MEDIUM=0
VULN_LOW=0
SCAN_PASSED=true

# ==========================================
# 前置检查
# ==========================================
check_tools() {
    log_step "检查安全扫描工具..."

    # 检查/安装 Trivy
    if ! command -v trivy &>/dev/null; then
        log_warn "Trivy 未安装，正在安装..."
        # 安装 Trivy
        if command -v apt-get &>/dev/null; then
            sudo apt-get update -qq && sudo apt-get install -y -qq wget apt-transport-https gnupg lsb-release
            wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
            echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
            sudo apt-get update -qq && sudo apt-get install -y -qq trivy
        elif command -v brew &>/dev/null; then
            brew install trivy
        else
            # 通用安装方式
            curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin
        fi
    fi

    # 检查/安装 Bandit
    if ! command -v bandit &>/dev/null; then
        log_warn "Bandit 未安装，正在安装..."
        pip3 install bandit 2>/dev/null || pip install bandit 2>/dev/null || {
            log_error "无法安装 Bandit，请手动安装: pip install bandit"
            exit 1
        }
    fi

    # 创建报告目录
    mkdir -p "${REPORT_DIR}"

    log_info "安全扫描工具检查通过"
}

# ==========================================
# Trivy - Docker镜像漏洞扫描
# ==========================================
scan_images() {
    log_step "===== Docker 镜像漏洞扫描 (Trivy) ====="

    local image_list
    image_list=$(docker images --format '{{.Repository}}:{{.Tag}}' | grep -E 'langtou|langtou-' | head -20)

    if [[ -z "${image_list}" ]]; then
        log_warn "未找到 Langtou 相关的 Docker 镜像"
        return
    fi

    local image_report="${REPORT_DIR}/trivy-images-${SCAN_DATE}.json"

    echo "${image_list}" | while read -r image; do
        log_info "扫描镜像: ${image}"

        # Trivy 扫描
        trivy image \
            --severity "${TRIVY_SEVERITY}" \
            --format json \
            --output "${image_report}" \
            "${image}" 2>/dev/null || true

        # 统计漏洞数量
        local critical high medium low
        critical=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${image_report}" 2>/dev/null || echo 0)
        high=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${image_report}" 2>/dev/null || echo 0)
        medium=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "MEDIUM")] | length' "${image_report}" 2>/dev/null || echo 0)
        low=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "LOW")] | length' "${image_report}" 2>/dev/null || echo 0)

        log_info "  ${image} - CRITICAL: ${critical}, HIGH: ${high}, MEDIUM: ${medium}, LOW: ${low}"

        if [[ "${critical}" -gt 0 ]]; then
            log_error "  发现 ${critical} 个 CRITICAL 漏洞!"
            SCAN_PASSED=false
        fi
        if [[ "${high}" -gt 0 ]]; then
            log_warn "  发现 ${high} 个 HIGH 漏洞"
            SCAN_PASSED=false
        fi
    done
}

# ==========================================
# Trivy - 文件系统/依赖漏洞扫描
# ==========================================
scan_dependencies() {
    log_step "===== 依赖漏洞扫描 (Trivy fs) ====="

    local dep_report="${REPORT_DIR}/trivy-dependencies-${SCAN_DATE}.json"

    # 扫描项目依赖
    if [[ -d "${PROJECT_DIR}/langtou-gateway" ]]; then
        log_info "扫描 Gateway 服务依赖..."
        trivy fs \
            --severity "${TRIVY_SEVERITY}" \
            --format json \
            --output "${dep_report}" \
            "${PROJECT_DIR}/langtou-gateway" 2>/dev/null || true

        local critical high
        critical=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${dep_report}" 2>/dev/null || echo 0)
        high=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${dep_report}" 2>/dev/null || echo 0)
        log_info "  Gateway - CRITICAL: ${critical}, HIGH: ${high}"
    fi

    # 扫描所有子项目
    for dir in "${PROJECT_DIR}"/langtou-*/; do
        if [[ -d "${dir}" ]]; then
            local service_name
            service_name=$(basename "${dir}")
            local svc_report="${REPORT_DIR}/trivy-${service_name}-${SCAN_DATE}.json"

            log_info "扫描 ${service_name} 依赖..."
            trivy fs \
                --severity "${TRIVY_SEVERITY}" \
                --format json \
                --output "${svc_report}" \
                "${dir}" 2>/dev/null || true

            local critical high
            critical=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "CRITICAL")] | length' "${svc_report}" 2>/dev/null || echo 0)
            high=$(jq '[.Results[].Vulnerabilities[]? | select(.Severity == "HIGH")] | length' "${svc_report}" 2>/dev/null || echo 0)
            log_info "  ${service_name} - CRITICAL: ${critical}, HIGH: ${high}"

            if [[ "${critical}" -gt 0 ]]; then
                log_error "  ${service_name} 发现 ${critical} 个 CRITICAL 漏洞!"
                SCAN_PASSED=false
            fi
        fi
    done
}

# ==========================================
# Trivy - IaC 配置扫描 (K8s/Docker)
# ==========================================
scan_iac() {
    log_step "===== 基础设施配置扫描 (Trivy IaC) ====="

    local iac_report="${REPORT_DIR}/trivy-iac-${SCAN_DATE}.json"

    if [[ -d "${PROJECT_DIR}/langtou-devops" ]]; then
        log_info "扫描 K8s/Docker 配置文件..."
        trivy config \
            --severity "${TRIVY_SEVERITY}" \
            --format json \
            --output "${iac_report}" \
            "${PROJECT_DIR}/langtou-devops" 2>/dev/null || true

        local critical high
        critical=$(jq '[.Results[]?.Misconfigurations[]? | select(.Severity == "CRITICAL")] | length' "${iac_report}" 2>/dev/null || echo 0)
        high=$(jq '[.Results[]?.Misconfigurations[]? | select(.Severity == "HIGH")] | length' "${iac_report}" 2>/dev/null || echo 0)
        log_info "  IaC 配置 - CRITICAL: ${critical}, HIGH: ${high}"

        if [[ "${critical}" -gt 0 ]]; then
            log_error "  IaC 配置发现 ${critical} 个 CRITICAL 问题!"
            SCAN_PASSED=false
        fi
    fi
}

# ==========================================
# Bandit - Python 代码安全扫描
# ==========================================
scan_python() {
    log_step "===== Python 代码安全扫描 (Bandit) ====="

    local python_report="${REPORT_DIR}/bandit-${SCAN_DATE}.json"

    # 查找所有 Python 项目
    local python_dirs=()
    for dir in "${PROJECT_DIR}"/langtou-*/; do
        if [[ -d "${dir}" ]] && find "${dir}" -name "*.py" -print -quit | grep -q .; then
            python_dirs+=("${dir}")
        fi
    done

    # 扫描 devops 脚本
    if [[ -d "${PROJECT_DIR}/langtou-devops/scripts" ]]; then
        python_dirs+=("${PROJECT_DIR}/langtou-devops/scripts")
    fi

    if [[ ${#python_dirs[@]} -eq 0 ]]; then
        log_warn "未找到 Python 代码文件"
        return
    fi

    for dir in "${python_dirs[@]}"; do
        local service_name
        service_name=$(basename "${dir}")
        log_info "扫描 Python 代码: ${service_name}"

        bandit -r "${dir}" \
            -f json \
            -o "${python_report}" \
            -ll \
            --exclude tests \
            2>/dev/null || true

        # 统计问题
        local high medium low
        high=$(jq '[.results[] | select(.issue_severity == "HIGH")] | length' "${python_report}" 2>/dev/null || echo 0)
        medium=$(jq '[.results[] | select(.issue_severity == "MEDIUM")] | length' "${python_report}" 2>/dev/null || echo 0)
        low=$(jq '[.results[] | select(.issue_severity == "LOW")] | length' "${python_report}" 2>/dev/null || echo 0)
        log_info "  ${service_name} - HIGH: ${high}, MEDIUM: ${medium}, LOW: ${low}"

        if [[ "${high}" -gt 0 ]]; then
            log_error "  ${service_name} 发现 ${high} 个 HIGH 安全问题!"
            SCAN_PASSED=false
        fi
    done
}

# ==========================================
# 密钥泄露检测
# ==========================================
scan_secrets() {
    log_step "===== 密钥/敏感信息泄露检测 ====="

    # 检查是否有硬编码的密钥
    local secret_patterns=(
        "password\s*=\s*['\"][^'\"]+['\"]"
        "secret\s*=\s*['\"][^'\"]+['\"]"
        "api[_-]?key\s*=\s*['\"][^'\"]+['\"]"
        "token\s*=\s*['\"][^'\"]+['\"]"
        "aws_access_key_id"
        "aws_secret_access_key"
        "private_key"
        "BEGIN RSA PRIVATE KEY"
    )

    local found_secrets=false

    for pattern in "${secret_patterns[@]}"; do
        local matches
        matches=$(grep -rn "${pattern}" \
            --include="*.java" \
            --include="*.py" \
            --include="*.yml" \
            --include="*.yaml" \
            --include="*.properties" \
            --include="*.conf" \
            "${PROJECT_DIR}" 2>/dev/null | grep -v "test" | grep -v ".env.example" | head -5) || true

        if [[ -n "${matches}" ]]; then
            log_error "发现可能的硬编码密钥 (pattern: ${pattern}):"
            echo "${matches}" | while read -r line; do
                log_error "  ${line}"
            done
            found_secrets=true
        fi
    done

    if [[ "${found_secrets}" == "true" ]]; then
        log_error "检测到硬编码密钥，请使用 K8s Secret 或环境变量管理!"
        SCAN_PASSED=false
    else
        log_info "未检测到硬编码密钥"
    fi
}

# ==========================================
# 生成汇总报告
# ==========================================
generate_report() {
    log_step "===== 生成安全扫描汇总报告 ====="

    local summary_report="${REPORT_DIR}/security-summary-${SCAN_DATE}.md"

    cat > "${summary_report}" <<EOF
# Langtou 安全扫描报告

**扫描时间**: $(date '+%Y-%m-%d %H:%M:%S')
**扫描结果**: ${SCAN_PASSED == true && "PASS" || "FAIL"}

## 扫描范围

| 扫描类型 | 工具 | 说明 |
|----------|------|------|
| Docker 镜像 | Trivy | 容器镜像漏洞扫描 |
| 依赖漏洞 | Trivy fs | 项目依赖漏洞扫描 |
| IaC 配置 | Trivy config | K8s/Docker 配置安全扫描 |
| Python 代码 | Bandit | Python 代码安全扫描 |
| 密钥泄露 | Grep | 硬编码密钥检测 |

## 扫描结果

EOF

    # 附加各扫描工具的详细报告
    for report_file in "${REPORT_DIR}"/*-${SCAN_DATE}.json; do
        if [[ -f "${report_file}" ]]; then
            local report_name
            report_name=$(basename "${report_file}")
            echo "### ${report_name}" >> "${summary_report}"
            echo "" >> "${summary_report}"
            echo '```json' >> "${summary_report}"
            cat "${report_file}" >> "${summary_report}"
            echo '```' >> "${summary_report}"
            echo "" >> "${summary_report}"
        fi
    done

    if [[ "${SCAN_PASSED}" == "true" ]]; then
        echo "## 结论: 所有安全扫描通过" >> "${summary_report}"
    else
        echo "## 结论: 存在安全问题，请查看上方详细报告并修复" >> "${summary_report}"
    fi

    log_info "汇总报告已生成: ${summary_report}"
}

# ==========================================
# 使用帮助
# ==========================================
usage() {
    echo "用法: $0 [options]"
    echo ""
    echo "选项:"
    echo "  --full          执行完整扫描 (镜像+依赖+IaC+Python+密钥)"
    echo "  --images        仅扫描 Docker 镜像"
    echo "  --deps          仅扫描项目依赖"
    echo "  --iac           仅扫描 IaC 配置"
    echo "  --python        仅扫描 Python 代码"
    echo "  --secrets       仅扫描密钥泄露"
    echo "  --report <dir>  指定报告输出目录 (默认: ${REPORT_DIR})"
    echo "  --fix           尝试自动修复 (仅限依赖更新)"
    echo "  -h, --help      显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --full                    # 完整扫描"
    echo "  $0 --images --deps           # 扫描镜像和依赖"
    echo "  $0 --full --report /tmp/scan # 指定报告目录"
}

# ==========================================
# 主入口
# ==========================================
main() {
    # 创建日志目录
    mkdir -p "$(dirname "${LOG_FILE}")" 2>/dev/null || LOG_FILE="/tmp/security-scan-${SCAN_DATE}.log"
    mkdir -p "${REPORT_DIR}"

    local scan_images=true
    local scan_deps=true
    local scan_iac=true
    local scan_python=true
    local scan_secrets=true
    local auto_fix=false

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --full)
                shift
                ;;
            --images)
                scan_deps=false
                scan_iac=false
                scan_python=false
                scan_secrets=false
                shift
                ;;
            --deps)
                scan_images=false
                scan_iac=false
                scan_python=false
                scan_secrets=false
                shift
                ;;
            --iac)
                scan_images=false
                scan_deps=false
                scan_python=false
                scan_secrets=false
                shift
                ;;
            --python)
                scan_images=false
                scan_deps=false
                scan_iac=false
                scan_secrets=false
                shift
                ;;
            --secrets)
                scan_images=false
                scan_deps=false
                scan_iac=false
                scan_python=false
                shift
                ;;
            --report)
                REPORT_DIR="$2"
                mkdir -p "${REPORT_DIR}"
                shift 2
                ;;
            --fix)
                auto_fix=true
                shift
                ;;
            -h|--help|help)
                usage
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                usage
                exit 1
                ;;
        esac
    done

    echo ""
    echo "=========================================="
    echo "  Langtou 安全扫描"
    echo "  扫描时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  报告目录: ${REPORT_DIR}"
    echo "=========================================="
    echo ""

    # 前置检查
    check_tools

    # 执行扫描
    if [[ "${scan_images}" == "true" ]]; then
        scan_images
    fi

    if [[ "${scan_deps}" == "true" ]]; then
        scan_dependencies
    fi

    if [[ "${scan_iac}" == "true" ]]; then
        scan_iac
    fi

    if [[ "${scan_python}" == "true" ]]; then
        scan_python
    fi

    if [[ "${scan_secrets}" == "true" ]]; then
        scan_secrets
    fi

    # 自动修复
    if [[ "${auto_fix}" == "true" ]]; then
        log_step "尝试自动修复..."
        # Trivy 自动修复依赖漏洞
        if command -v trivy &>/dev/null; then
            for dir in "${PROJECT_DIR}"/langtou-*/; do
                if [[ -d "${dir}" ]]; then
                    log_info "尝试修复 $(basename "${dir}") 依赖..."
                    trivy fs --fix --auto-fix "${dir}" 2>/dev/null || true
                fi
            done
        fi
    fi

    # 生成汇总报告
    generate_report

    # 最终结果
    echo ""
    echo "=========================================="
    if [[ "${SCAN_PASSED}" == "true" ]]; then
        echo -e "${GREEN}  安全扫描结果: PASS${NC}"
    else
        echo -e "${RED}  安全扫描结果: FAIL - 请查看报告并修复问题${NC}"
    fi
    echo "  报告目录: ${REPORT_DIR}"
    echo "=========================================="
    echo ""

    if [[ "${SCAN_PASSED}" == "false" ]]; then
        exit 1
    fi
}

main "$@"
