﻿﻿﻿#!/usr/bin/env bash
# =============================================================================
# 榔头 Quiz MVP - 端到端联调测试脚本 (Bash)
# 用法：bash quiz-e2e-test.sh [quiz_base_url] [gateway_base_url]
# 默认：quiz_base_url=http://localhost:8089  gateway_base_url=http://localhost:8080
# =============================================================================
set -euo pipefail

QUIZ_BASE="${1:-http://localhost:8089}"
GATEWAY_BASE="${2:-http://localhost:8080}"
PASS=0
FAIL=0
TOTAL=0

red()    { echo -e "\033[31m$1\033[0m"; }
green()  { echo -e "\033[32m$1\033[0m"; }
yellow() { echo -e "\033[33m$1\033[0m"; }

log_title() {
    echo
    yellow "================================================"
    yellow " $1"
    yellow "================================================"
}

assert_status() {
    local name="$1" expected="$2" actual="$3"
    TOTAL=$((TOTAL + 1))
    if [[ "$actual" == "$expected" ]]; then
        PASS=$((PASS + 1))
        green "[PASS] $name  (HTTP $actual)"
        return 0
    else
        FAIL=$((FAIL + 1))
        red   "[FAIL] $name  expected HTTP $expected, got $actual"
        return 1
    fi
}

assert_code() {
    local name="$1" expected="$2" body="$3"
    local code
    code=$(echo "$body" | grep -o '"code":[0-9]*' | head -1 | grep -o '[0-9]*')
    TOTAL=$((TOTAL + 1))
    if [[ "$code" == "$expected" ]]; then
        PASS=$((PASS + 1))
        green "[PASS] $name  (code=$code)"
        return 0
    else
        FAIL=$((FAIL + 1))
        red   "[FAIL] $name  expected code=$expected, got code=$code"
        echo "       body: $body"
        return 1
    fi
}

pretty() {
    if command -v jq >/dev/null 2>&1; then
        echo "$1" | jq .
    else
        echo "$1"
    fi
}

http() {
    local method="$1" url="$2" data="${3:-}" headers="${4:-}"
    local args=(-s -o /tmp/quiz_body.$$ -w "%{http_code}" -X "$method")
    if [[ -n "$data" ]]; then
        args+=(-H "Content-Type: application/json" -d "$data")
    fi
    if [[ -n "$headers" ]]; then
        args+=(-H "$headers")
    fi
    local code
    code=$(curl "${args[@]}" "$url")
    local body
    body=$(cat /tmp/quiz_body.$$)
    rm -f /tmp/quiz_body.$$
    echo "$code|$body"
}

# =============================================================================
# 0. 健康检查
# =============================================================================
log_title "0. 健康检查 (Health Check)"

resp=$(http GET "$QUIZ_BASE/actuator/health")
code=${resp%%|*}
assert_status "Quiz Service /actuator/health" "200" "$code"
body=${resp#*|}
pretty "$body"

resp=$(http GET "$GATEWAY_BASE/actuator/health")
code=${resp%%|*}
assert_status "Gateway /actuator/health" "200" "$code"

for port in 8082 8081; do
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" >/dev/null 2>&1; then
        green "[INFO] 端口 $port 可达"
    else
        yellow "[WARN] 端口 $port 未启动"
    fi
done

# =============================================================================
# 1. 生成关卡
# =============================================================================
log_title "1. 生成关卡"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/generate" '{"noteId": 1, "questionCount": 10}' "X-User-Id: 1")
code=${resp%%|*}
body=${resp#*|}
assert_status "1.1 POST /quiz/generate" "200" "$code"
assert_code   "1.2 generate 返回 code=200" "200" "$body"

QUIZ_SET_ID=$(echo "$body" | grep -o '"quizSetId":[0-9]*' | head -1 | grep -o '[0-9]*')
green "生成的 quizSetId = $QUIZ_SET_ID"

if [[ -z "$QUIZ_SET_ID" ]]; then
    red "无法获取 quizSetId, 终止测试"
    exit 1
fi

# =============================================================================
# 2. 获取关卡详情
# =============================================================================
log_title "2. 获取关卡详情"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/sets/$QUIZ_SET_ID")
code=${resp%%|*}
body=${resp#*|}
assert_status "2.1 GET /quiz/sets/$QUIZ_SET_ID" "200" "$code"
assert_code   "2.2 sets 返回 code=200" "200" "$body"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/sets/$QUIZ_SET_ID")
code=${resp%%|*}
assert_status "2.3 第二次读 (命中缓存)" "200" "$code"

# =============================================================================
# 3. 开始答题
# =============================================================================
log_title "3. 开始答题"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/attempts" "{\"quizSetId\": $QUIZ_SET_ID}" "X-User-Id: 2")
code=${resp%%|*}
body=${resp#*|}
assert_status "3.1 POST /quiz/attempts" "200" "$code"
assert_code   "3.2 attempts 返回 code=200" "200" "$body"

ATTEMPT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
green "生成的 attemptId = $ATTEMPT_ID"

if [[ -z "$ATTEMPT_ID" ]]; then
    red "无法获取 attemptId, 终止测试"
    exit 1
fi

# =============================================================================
# 4. 获取答题记录
# =============================================================================
log_title "4. 获取答题记录"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/attempts/$ATTEMPT_ID" "" "X-User-Id: 2")
code=${resp%%|*}
body=${resp#*|}
assert_status "4.1 GET /quiz/attempts/$ATTEMPT_ID" "200" "$code"
assert_code   "4.2 attempts/{id} 返回 code=200" "200" "$body"

# =============================================================================
# 5. 提交答案
# =============================================================================
log_title "5. 提交答案"

SAMPLE='{"answers":[{"sequenceNo":1,"selected":"A"},{"sequenceNo":2,"selected":"B"},{"sequenceNo":3,"selected":"C"},{"sequenceNo":4,"selected":"D"},{"sequenceNo":5,"selected":"A"}],"durationSeconds":25}'

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/attempts/$ATTEMPT_ID/submit" "$SAMPLE" "X-User-Id: 2")
code=${resp%%|*}
body=${resp#*|}
assert_status "5.1 POST /quiz/attempts/$ATTEMPT_ID/submit" "200" "$code"
assert_code   "5.2 submit 返回 code=200" "200" "$body"

# 重复提交（幂等校验）
resp2=$(http POST "$QUIZ_BASE/api/v1/quiz/attempts/$ATTEMPT_ID/submit" "$SAMPLE" "X-User-Id: 2")
code2=${resp2%%|*}
body2=${resp2#*|}
assert_status "5.3 重复提交 (幂等)" "200" "$code2"
SECOND_CODE=$(echo "$body2" | grep -o '"code":[0-9]*' | head -1 | grep -o '[0-9]*')
if [[ "$SECOND_CODE" == "200" ]]; then
    yellow "[INFO] 重复提交未被拦截 (可能锁已过期或业务允许) code=$SECOND_CODE"
else
    green "[INFO] 幂等锁正确拦截重复提交 code=$SECOND_CODE"
fi

# =============================================================================
# 6. 我的关卡列表
# =============================================================================
log_title "6. 我的关卡列表"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/sets/my?page=1&size=10" "" "X-User-Id: 1")
code=${resp%%|*}
body=${resp#*|}
assert_status "6.1 GET /quiz/sets/my" "200" "$code"
assert_code   "6.2 sets/my 返回 code=200" "200" "$body"

# =============================================================================
# 7. 我的答题历史
# =============================================================================
log_title "7. 我的答题历史"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/attempts/my?page=1&size=10" "" "X-User-Id: 2")
code=${resp%%|*}
body=${resp#*|}
assert_status "7.1 GET /quiz/attempts/my" "200" "$code"
assert_code   "7.2 attempts/my 返回 code=200" "200" "$body"

# =============================================================================
# 8. 全局排行榜
# =============================================================================
log_title "8. 全局排行榜"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/leaderboard/global?limit=20")
code=${resp%%|*}
body=${resp#*|}
assert_status "8.1 GET /quiz/leaderboard/global" "200" "$code"
assert_code   "8.2 leaderboard/global 返回 code=200" "200" "$body"

# =============================================================================
# 9. 关卡排行榜
# =============================================================================
log_title "9. 关卡排行榜"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/leaderboard/quiz/$QUIZ_SET_ID?limit=20")
code=${resp%%|*}
body=${resp#*|}
assert_status "9.1 GET /quiz/leaderboard/quiz/$QUIZ_SET_ID" "200" "$code"
assert_code   "9.2 leaderboard/quiz 返回 code=200" "200" "$body"

# =============================================================================
# 10. 好友排行榜
# =============================================================================
log_title "10. 好友排行榜"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/leaderboard/friends?limit=20" "" "X-User-Id: 2")
code=${resp%%|*}
body=${resp#*|}
assert_status "10.1 GET /quiz/leaderboard/friends" "200" "$code"
assert_code   "10.2 leaderboard/friends 返回 code=200" "200" "$body"

# =============================================================================
# 11. 错误场景测试
# =============================================================================
log_title "11. 错误场景测试"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/generate" '{"noteId":1,"questionCount":10}')
code=${resp%%|*}
yellow "11.1 缺失 X-User-Id: HTTP $code (预期 400 或 500)"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/generate" '{"noteId":-1,"questionCount":10}' "X-User-Id: 1")
code=${resp%%|*}
yellow "11.2 非法 noteId: HTTP $code"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/generate" '{"noteId":1,"questionCount":3}' "X-User-Id: 1")
code=${resp%%|*}
yellow "11.3 questionCount 越界: HTTP $code"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/sets/99999")
code=${resp%%|*}
assert_status "11.4 关卡不存在 (404)" "404" "$code"

resp=$(http POST "$QUIZ_BASE/api/v1/quiz/attempts/$ATTEMPT_ID/submit" '{"answers":[{"sequenceNo":999,"selected":"A"}],"durationSeconds":10}' "X-User-Id: 2")
code=${resp%%|*}
yellow "11.5 题目序号越界: HTTP $code"

resp=$(http GET "$QUIZ_BASE/api/v1/quiz/attempts/$ATTEMPT_ID" "" "X-User-Id: 999")
code=${resp%%|*}
yellow "11.6 越权访问: HTTP $code (预期 400)"

# =============================================================================
# 汇总
# =============================================================================
log_title "测试汇总"
green "通过：$PASS / $TOTAL"
red   "失败：$FAIL / $TOTAL"

if [[ "$FAIL" -gt 0 ]]; then
    exit 1
fi
exit 0
