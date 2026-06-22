﻿﻿﻿# 榔头 Quiz MVP - Swagger API 测试清单

> 版本：v1.0
> 服务端点：
> - Gateway（聚合入口）：`http://localhost:8080`
> - Quiz Service（直连）：`http://localhost:8089`
> - AI Service：`http://localhost:8082`
> - Content Service：`http://localhost:8081`

## 1. 前置条件

| # | 条件 | 校验命令 |
|---|------|---------|
| 1 | MySQL 已启动，数据库 `langtou` 存在 | `mysql -uroot -proot -e "SHOW DATABASES LIKE 'langtou';"` |
| 2 | Redis 已启动 | `redis-cli PING` -> `PONG` |
| 3 | Nacos 已启动（端口 8848） | 浏览器访问 `http://localhost:8848/nacos` |
| 4 | Gateway 已注册到 Nacos | `curl http://localhost:8080/actuator/health` |
| 5 | Quiz Service 已注册到 Nacos | `curl http://localhost:8089/actuator/health` |
| 6 | AI Service 已注册到 Nacos | `curl http://localhost:8082/actuator/health` |
| 7 | Content Service 已注册到 Nacos | `curl http://localhost:8081/actuator/health` |
| 8 | Flyway 迁移已完成（V20、V21、V22） | `SELECT * FROM flyway_schema_history WHERE version IN ('V20','V21','V22');` |
| 9 | 测试账号已初始化（userId=1 出题者，userId=2 答题者） | `SELECT id, nickname FROM user WHERE id IN (1,2);` |

## 2. 基础健康检查

```bash
# Gateway
curl -s http://localhost:8080/actuator/health
# 预期：{"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"},...}}

# Quiz Service
curl -s http://localhost:8089/actuator/health
# 预期：{"status":"UP"}

# AI Service
curl -s http://localhost:8082/actuator/health

# Content Service
curl -s http://localhost:8081/actuator/health
```

## 3. 鉴权说明

- Quiz Service 内部 Controller 通过 `@RequestHeader("X-User-Id")` 识别当前用户。
- Gateway 会把 JWT 解析后的用户信息透传为 `X-User-Id` Header。
- **直连 Quiz Service 测试**需手动设置 `X-User-Id: <userId>`。
- 通过 Gateway 访问需携带 `Authorization: Bearer <token>`。

测试场景下推荐直连 Quiz Service（端口 8089）以绕过 JWT 依赖。

## 4. Quiz Service API 测试清单（10 个端点）

### 4.1 生成关卡 `POST /api/v1/quiz/generate`

请求：
```bash
curl -s -X POST http://localhost:8089/api/v1/quiz/generate \
  -H "X-User-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"noteId": 1, "questionCount": 10}'
```

预期响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "quizSetId": 1,
    "noteId": 1,
    "questionCount": 10,
    "status": "READY",
    "questions": [
      {
        "sequenceNo": 1,
        "stem": "题干…",
        "optionA": "A", "optionB": "B", "optionC": "C", "optionD": "D",
        "correctAnswer": "A",
        "explanation": "解析…"
      }
    ]
  }
}
```

校验点：
- `data.quizSetId` 为自增 ID
- `data.status === "READY"`
- `data.questionCount === questions.length`
- `correctAnswer` 为 `A/B/C/D` 之一

---

### 4.2 获取关卡详情 `GET /api/v1/quiz/sets/{id}`

请求：
```bash
curl -s http://localhost:8089/api/v1/quiz/sets/1
```

预期响应：`code=200`，返回 QuizSet。首次写 Redis，二次命中缓存。

---

### 4.3 开始答题 `POST /api/v1/quiz/attempts`

请求：
```bash
curl -s -X POST http://localhost:8089/api/v1/quiz/attempts \
  -H "X-User-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{"quizSetId": 1}'
```

预期响应：`status=IN_PROGRESS`，`livesLeft=3`。

---

### 4.4 提交答案 `POST /api/v1/quiz/attempts/{id}/submit`

请求：
```bash
curl -s -X POST http://localhost:8089/api/v1/quiz/attempts/1/submit \
  -H "X-User-Id: 2" \
  -H "Content-Type: application/json" \
  -d '{"answers":[{"sequenceNo":1,"selected":"A"},{"sequenceNo":2,"selected":"B"}],"durationSeconds":25}'
```

预期响应：`rank` 星级映射（≥90%→3，≥70%→2，≥50%→1，否则 0）。

---

### 4.5 获取答题记录 `GET /api/v1/quiz/attempts/{id}`

请求：
```bash
curl -s http://localhost:8089/api/v1/quiz/attempts/1 -H "X-User-Id: 2"
```

---

### 4.6 我的关卡列表 `GET /api/v1/quiz/sets/my`

请求：
```bash
curl -s "http://localhost:8089/api/v1/quiz/sets/my?page=1&size=10" -H "X-User-Id: 1"
```

---

### 4.7 我的答题历史 `GET /api/v1/quiz/attempts/my`

请求：
```bash
curl -s "http://localhost:8089/api/v1/quiz/attempts/my?page=1&size=10" -H "X-User-Id: 2"
```

---

### 4.8 全局排行榜 `GET /api/v1/quiz/leaderboard/global`

请求：
```bash
curl -s "http://localhost:8089/api/v1/quiz/leaderboard/global?limit=20"
```

---

### 4.9 关卡排行榜 `GET /api/v1/quiz/leaderboard/quiz/{setId}`

请求：
```bash
curl -s "http://localhost:8089/api/v1/quiz/leaderboard/quiz/1?limit=20"
```

---

### 4.10 好友排行榜 `GET /api/v1/quiz/leaderboard/friends`

请求：
```bash
curl -s "http://localhost:8089/api/v1/quiz/leaderboard/friends?limit=20" -H "X-User-Id: 2"
```

## 5. 错误场景测试

| # | 场景 | 请求 | 预期响应 |
|---|------|------|---------|
| E1 | 缺失 `X-User-Id` Header | `POST /api/v1/quiz/generate` 不带 Header | 400 Bad Request / 500 |
| E2 | 非法 noteId | `{"noteId":-1,"questionCount":10}` | 400 或 AI 失败 |
| E3 | questionCount 越界 | `questionCount: 3` | 400 参数校验失败 |
| E4 | 关卡不存在 | `GET /sets/99999` | 404 `关卡不存在` |
| E5 | 不可用关卡 | `status=FAILED` 的 set 开始答题 | 400 `关卡不可用` |
| E6 | 重复提交 | 同一 attemptId 调两次 submit | 400 `请勿重复提交` |
| E7 | 超时提交 | `durationSeconds > total*60` | 400 `答题超时` |
| E8 | 题目序号越界 | `sequenceNo: 999` | 400 `题目序号超出范围` |
| E9 | 越权访问 | 用他人 userId 查 attempt | 400 `无权访问` |
| E10 | AI 熔断 | 关 AI 服务后调 `/generate` | 500 `AI 生成题目失败` |

## 6. 联调完成判据

- [ ] 全部 10 个端点返回 200，字段符合预期
- [ ] 10 个错误场景均被正确拦截
- [ ] Redis 可查 `quiz:set:*` 缓存与 `quiz:attempt:*:lock` 幂等锁
- [ ] MySQL `quiz_set / quiz_question / quiz_attempt` 数据正确
- [ ] Zipkin 可串联 `Gateway → Quiz → AI` 调用链
