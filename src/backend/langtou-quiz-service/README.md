# langtou-quiz-service

榔头 AI + UGC 互动答题 / 知识闯关 MVP 核心服务。

## 1. 服务简介

`langtou-quiz-service` 是榔头后端微服务体系中承载「笔记 → 闯关」玩法的核心服务，职责如下：

- **题库（QuizSet）生命周期管理**：创建、状态流转（DRAFT / PUBLISHED / ARCHIVED）、归档
- **题目（QuizQuestion）CRUD**：接收 AI Service 生成结果并落库，保障题目质量与解析完整
- **答题记录（QuizAttempt）**：记录玩家闯关过程、得分、剩余生命值、续命次数与通关状态
- **排行榜服务（LeaderboardService）**：提供总榜 / 单关卡榜 / 好友榜三类榜单，基于 Redis ZSet 高性能读写
- **降级保护**：内置 NONE / L1 / L2 / L3 四级降级等级，当 AI 或下游服务异常时自动熔断
- **对外支撑**：
  - 为 Game Service 提供「答题会话」数据支撑
  - 为 Content Service 提供「笔记 ↔ 关卡」关联查询
  - 为 Creator Service 提供周结算所需的归因数据

## 2. 技术栈

| 类别 | 技术 | 版本 / 说明 |
|------|------|-------------|
| 语言 / 框架 | Java 21 + Spring Boot | 父 POM `langtou-backend:7.0.0-RELEASE` |
| 服务治理 | Spring Cloud Alibaba Nacos | 注册发现、服务名 `langtou-quiz-service` |
| RPC 调用 | Spring Cloud OpenFeign | 调用 `langtou-ai-service` 生成题目 |
| ORM | MyBatis-Plus | MySQL 访问 + 乐观锁（`@Version`） |
| 数据库 | MySQL 8.x | 数据库 `langtou` |
| 缓存 / 排行榜 | Redis（Lettuce） | QuizSet 缓存 + ZSet 排行榜 + 提交锁 |
| 接口文档 | springdoc-openapi（Swagger UI） | 版本 2.3.0 |
| 可观测 | Spring Boot Actuator + Micrometer + Zipkin | Prometheus / 健康检查 / Trace 全链路 |
| 数据库迁移 | Flyway | `classpath:db/migration` |
| 测试 | JUnit 5 + Mockito + H2 | 单元 / 集成测试 |

## 3. 端口号

| 端口 | 用途 | 说明 |
|------|------|------|
| `8089` | HTTP 业务端口 | `server.port` |
| `8089/actuator` | 健康检查 / 指标 | `management.endpoints.web.exposure.include=health,info,metrics,prometheus` |
| `6379` | Redis 端口 | Lettuce 客户端连接 |
| `3306` | MySQL 端口 | 数据库 `langtou` |
| `8848` | Nacos 端口 | 默认 `localhost:8848`，可通过 `NACOS_SERVER` 环境变量覆盖 |

## 4. 启动方式

### 4.1 本地启动（依赖已启动的 Nacos / MySQL / Redis / AI Service）

```bash
# 方式一：在仓库根目录启动（推荐，解决模块依赖）
mvn -pl langtou-quiz-service -am spring-boot:run

# 方式二：仅启动本模块（需提前 mvn install langtou-common 到本地仓库）
cd langtou-backend/langtou-quiz-service
mvn spring-boot:run
```

### 4.2 关键环境变量

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_PASSWORD` | `root` | MySQL 密码 |
| `NACOS_SERVER` | `localhost:8848` | Nacos 地址 |
| `NACOS_NAMESPACE` | 空字符串 | Nacos 命名空间 |
| `ZIPKIN_HOST` | `localhost` | Zipkin 主机 |
| `ZIPKIN_PORT` | `9411` | Zipkin 端口 |

### 4.3 打包

```bash
mvn -pl langtou-quiz-service -am clean package -DskipTests
java -jar target/langtou-quiz-service-7.0.0-RELEASE.jar
```

## 5. 依赖服务

| 依赖服务 | 服务名 | 调用方向 | 说明 |
|----------|--------|----------|------|
| **AI Service** | `langtou-ai-service` | Feign 客户端 | 调用 `/api/v1/ai/quiz/generate` 生成 10 道选择题，带 Fallback 降级 |
| **MySQL** | - | 持久化 | `quiz_set` / `quiz_question` / `quiz_attempt` 三张核心表 |
| **Redis** | - | 缓存 / 排行榜 | QuizSet 缓存（30min TTL）、提交锁（10min TTL）、ZSet 排行榜 |
| **Nacos** | - | 服务注册发现 | 注册 `langtou-quiz-service` 供其他服务调用 |
| **Zipkin** | - | 链路追踪 | 上报 TraceId 便于排查跨服务问题 |
| **Gateway** | `langtou-gateway` | 被调用 | 由 Gateway 通过 JWT 鉴权后转发请求到本服务 |

## 6. API 端点清单

### 6.1 QuizController（`/api/v1/quiz`）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| `POST` | `/api/v1/quiz/generate` | `X-User-Id` Header | 基于笔记一键生成关卡（调用 AI Service） |
| `GET` | `/api/v1/quiz/sets/{id}` | 公开 | 查询单个题库详情（含题目列表） |
| `POST` | `/api/v1/quiz/attempts` | `X-User-Id` Header | 玩家开始一局答题，创建 `QuizAttempt` 记录 |
| `POST` | `/api/v1/quiz/attempts/{id}/submit` | `X-User-Id` Header | 提交答卷，返回得分 / 通关结果 / 剩余生命 |
| `GET` | `/api/v1/quiz/attempts/{id}` | `X-User-Id` Header | 查询单局答题记录（仅限本人） |
| `GET` | `/api/v1/quiz/sets/my` | `X-User-Id` Header | 分页查询"我创建的题库"（创作者视角） |
| `GET` | `/api/v1/quiz/attempts/my` | `X-User-Id` Header | 分页查询"我的答题历史"（玩家视角） |

### 6.2 LeaderboardController（`/api/v1/quiz/leaderboard`）

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| `GET` | `/api/v1/quiz/leaderboard/global` | 公开 | 获取总榜（`limit` 默认 20） |
| `GET` | `/api/v1/quiz/leaderboard/quiz/{setId}` | 公开 | 获取指定关卡的排行榜 |
| `GET` | `/api/v1/quiz/leaderboard/friends` | `X-User-Id` Header | 获取当前用户的好友榜 |

### 6.3 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

分页响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [ ... ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

## 7. 数据库表清单

| 表名 | 对应实体 | 说明 | 关键字段 |
|------|----------|------|----------|
| `quiz_set` | `QuizSet` | 题库（一个笔记对应一个关卡） | `id`, `note_id`, `creator_id`, `title`, `status`(`DRAFT/PUBLISHED/ARCHIVED`), `question_count`, `correct_rate`, `tags`(JSON) |
| `quiz_question` | `QuizQuestion` | 题目（10 道/套） | `id`, `quiz_set_id`, `sequence_no`, `stem`, `optionA`–`optionD`, `correct_answer`, `question_type`, `explanation`, `score` |
| `quiz_attempt` | `QuizAttempt` | 玩家答题记录 | `id`, `quiz_set_id`, `user_id`, `game_session_id`, `total_questions`, `correct_count`, `score`, `lives_left`, `revives_used`, `status`, `passed`, `duration_seconds`, `version`(乐观锁) |

Flyway 迁移脚本位置：`langtou-database/flyway/migrations/`（`V20__quiz_mvp.sql`、`V21__note_quiz_extension.sql`、`V22__quiz_fixes.sql`）。

## 8. Redis Key 约定

### 8.1 QuizCacheManager

| Key 模式 | 类型 | TTL | 用途 |
|----------|------|-----|------|
| `quiz:set:{setId}` | String（JSON） | 30 分钟 | 缓存 `QuizSet`（含题目），减少 MySQL 压力 |
| `quiz:attempt:{attemptId}:lock` | String（"1"） | 10 分钟 | 答题提交锁，防止重复提交 / 并发提交 |

### 8.2 LeaderboardServiceImpl

| Key 模式 | 类型 | TTL | 用途 |
|----------|------|-----|------|
| `lb:quiz:{setId}` | ZSet | 持久 | 单关卡排行榜，score = 玩家得分，member = userId |
| `lb:quiz:global` | ZSet | 持久 | 全局总榜 |
| `lb:quiz:friends:{userId}` | ZSet | 持久 | 好友榜（由好友关系变化时重建） |
| `lb:quiz:user:name:{userId}` | String | 持久 | 用户昵称缓存（展示榜单用，减少与 User Service 交互） |
| `lb:quiz:user:avatar:{userId}` | String | 持久 | 用户头像 URL 缓存 |

## 9. 测试命令

```bash
# 运行所有单元 + 集成测试
mvn -pl langtou-quiz-service -am test

# 仅运行单元测试（跳过集成测试）
mvn -pl langtou-quiz-service -am test -DskipITs

# 仅运行集成测试（带 @Tag 过滤，具体依测试类标签）
mvn -pl langtou-quiz-service -am verify -DskipUnitTests

# 指定测试类
mvn -pl langtou-quiz-service -am test -Dtest=QuizServiceIntegrationTest

# 测试覆盖率报告
mvn -pl langtou-quiz-service -am jacoco:report
```

测试使用 H2 内存数据库 + Testcontainers（如需要），测试脚本位于：
- `src/test/resources/schema.sql`
- `src/test/resources/data.sql`
- `src/test/resources/db/test_data.sql`

## 10. Swagger UI 地址

启动服务后访问：

- **Swagger UI**：`http://localhost:8089/swagger-ui.html`
- **OpenAPI JSON**：`http://localhost:8089/v3/api-docs`
- **Actuator Health**：`http://localhost:8089/actuator/health`
- **Prometheus 指标**：`http://localhost:8089/actuator/prometheus`

> 使用 Swagger UI 调试时，需在请求头携带 `X-User-Id`（数字）以模拟登录态；真实生产环境通过 Gateway JWT 鉴权。

## 11. 配置项速查（`quiz.*` 前缀）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `quiz.question.default-count` | 10 | 默认题目数（MVP 固定） |
| `quiz.question.min-count` | 5 | 最少题目数 |
| `quiz.question.max-count` | 12 | 最多题目数 |
| `quiz.question.per-question-seconds` | 60 | 每题倒计时（秒） |
| `quiz.question.life-per-question` | 1 | 每题生命值 |
| `quiz.question.passing-score` | 7 | 通关所需正确题数 |
| `quiz.revive.max-per-game` | 2 | 单局最大续命次数 |
| `quiz.revive.price-fen` | 99 | 续命价格（分，0.99 元） |
| `quiz.degrade.level` | `NONE` | 降级等级（`NONE/L1/L2/L3`） |

## 12. 施工阶段说明

本模块为 MVP 第一阶段（地基）创建，详细施工路线图与交付验收标准见：

- `langtou-team-config/contract-delivery/construction-plan-quiz-mvp.md`
- `langtou-team-config/contract-delivery/requirements-quiz-mvp.md`
- `langtou-team-config/contract-delivery/impact-analysis-quiz-mvp.md`
- `langtou-team-config/contract-delivery/review-report-quiz-mvp.md`
