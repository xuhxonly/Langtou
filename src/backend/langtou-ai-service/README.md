# langtou-ai-service

榔头（Langtou）平台**AI 创作服务**，为创作者提供标题生成、封面推荐、标签推荐与草稿生成等 AI 辅助能力。

## 1. 服务简介

通过对接阿里云 DashScope（`qwen-turbo`）大模型，为榔头平台创作者提供一站式 AI 写作助手：智能标题、标签推荐、草稿生成、封面推荐，并基于 Redis 实现按用户限流与结果缓存。

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 17 + Spring Boot 3.2.5 |
| 微服务治理 | Spring Cloud 2023.0.1 + Nacos Discovery |
| AI 框架 | Spring AI 1.0.0-M5（可选依赖，可通过 `langtou.ai.creation.provider` 切换到 `mock`） |
| HTTP 客户端 | `RestTemplate`（用于调用 DashScope / Mock Provider） |
| ORM | MyBatis-Plus 3.5.6（已引入，当前未定义 Entity，仅作预留） |
| 数据库 | MySQL 8.0（数据库：`langtou`） |
| 缓存 | Redis（Lettuce 客户端，用于限流与结果缓存） |
| 鉴权 | JJWT 0.12.5 + 自定义 `RoleCheckInterceptor` + `@RequireRole` |
| API 文档 | SpringDoc OpenAPI 2.3.0（Swagger UI） |
| 可观测性 | Spring Boot Actuator + Micrometer（Tracing / Prometheus）+ Zipkin |
| 构建 | Maven 3.9 + Spring Boot Plugin |
| 容器化 | Docker（Eclipse Temurin 17 JRE + G1GC） |

## 3. 端口号

| 端口 | 用途 |
|------|------|
| `8087` | HTTP / API |
| `8087` | Actuator（健康/指标） |

## 4. 启动方式

### 本地开发

```bash
# 前置：MySQL、Redis、Nacos 已启动
cd langtou-backend
mvn -pl langtou-common,langtou-ai-service -am spring-boot:run
```

或直接运行主类：`com.langtou.ai.AiServiceApplication`。

如需接入真实 AI Provider，设置以下环境变量：

```bash
export AI_CREATION_PROVIDER=dashscope
export AI_CREATION_API_KEY=sk-xxxx
```

### Docker

```bash
cd langtou-backend
mvn -pl langtou-common,langtou-ai-service -am clean package -DskipTests
docker build -t langtou-ai-service ./langtou-ai-service
docker run -d --name langtou-ai-service \
  -p 8087:8087 \
  -e DB_PASSWORD=root \
  -e NACOS_SERVER=localhost:8848 \
  -e AI_CREATION_PROVIDER=mock \
  langtou-ai-service
```

## 5. 依赖服务

本服务为**无状态 AI 能力层**，未直接使用 Feign 调用其他微服务；接入 Nacos 进行服务注册与发现。
可配置调用外部 AI Provider（默认 `mock`，生产推荐 `dashscope`）。

## 6. API 端点清单

### AI 创作接口（`AiCreationController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/ai/generate-title` | 基于图片/描述生成推荐标题 |
| POST | `/api/v1/ai/recommend-tags` | 根据标题/内容推荐标签 |
| POST | `/api/v1/ai/generate-draft` | 根据主题与风格生成草稿正文 |
| POST | `/api/v1/ai/recommend-cover` | 推荐封面图（基于图片/内容） |

> 所有接口需要在请求头携带 JWT（`@RequireRole`），并提供 `X-User-Id`。

## 7. 配置说明（`application.yml` 关键项）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8087` | 服务端口 |
| `spring.application.name` | `langtou-ai-service` | 注册到 Nacos 的服务名 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/langtou` | MySQL 连接 |
| `spring.datasource.password` | `${DB_PASSWORD:root}` | 数据库密码 |
| `spring.redis.host` / `port` | `localhost:6379` | Redis 地址（用于限流与缓存） |
| `spring.cloud.nacos.discovery.server-addr` | `${NACOS_SERVER:localhost:8848}` | Nacos 地址 |
| `langtou.ai.creation.enabled` | `true` | AI 创作开关 |
| `langtou.ai.creation.provider` | `${AI_CREATION_PROVIDER:mock}` | AI Provider（`mock` / `dashscope`） |
| `langtou.ai.creation.api-key` | `${AI_CREATION_API_KEY:}` | DashScope API Key |
| `langtou.ai.creation.api-url` | `https://dashscope.aliyuncs.com/api/v1/...` | DashScope 接口地址 |
| `langtou.ai.creation.model` | `qwen-turbo` | 使用的模型 |
| `langtou.ai.creation.rate-limit` | `10` | 单用户每秒限流次数 |
| `langtou.ai.creation.cache-ttl` | `3600` | 结果缓存 TTL（秒） |
| `mybatis-plus.type-aliases-package` | `com.langtou.ai.entity` | MyBatis-Plus 实体包（预留） |
| `management.zipkin.tracing.endpoint` | `http://localhost:9411` | Zipkin 上报地址 |

## 8. 数据库表清单

本服务当前为**无状态设计**，业务数据（请求/响应）不入库，只通过 Redis 做短期缓存与限流。
`pom.xml` 中已引入 MyBatis-Plus 与 MySQL 依赖，便于未来扩展 AI 调用审计、Prompt 模板管理等持久化场景。

> 后续版本计划新增 `ai_prompt_log`、`ai_usage_stat` 等表（位置：`com.langtou.ai.entity`）。

## 9. 测试命令

```bash
# 单元测试
mvn -pl langtou-ai-service test

# 打包跳过测试
mvn -pl langtou-ai-service -am clean package -DskipTests
```

> 当前 `src/test` 目录尚未包含测试类，测试体系将在后续 Sprint 中补齐。

## 10. Swagger UI 访问地址

- 本地：`http://localhost:8087/swagger-ui/index.html`
- API JSON：`http://localhost:8087/v3/api-docs`

## 附：健康检查

```bash
curl http://localhost:8087/actuator/health
curl http://localhost:8087/actuator/prometheus
```
