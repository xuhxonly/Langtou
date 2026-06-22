# langtou-game-service

榔头（Langtou）平台**游戏化服务**，为社区内容侧提供轻量游戏化扩展：对局房间、匹配、任务、道具背包、排行榜与游戏内支付。

## 1. 服务简介

面向榔头平台的游戏化扩展服务，基于 WebSocket（STOMP）实现实时对局通信，提供房间创建/加入、MMR 匹配、任务成就、道具背包、赛季排行榜及游戏内支付闭环。

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 17 + Spring Boot 3.2.5 |
| 微服务治理 | Spring Cloud 2023.0.1 + Nacos Discovery |
| 实时通信 | Spring WebSocket（STOMP over SockJS） |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8.0（数据库：`langtou`） |
| 缓存 | Redis（Lettuce 客户端，用于 MMR / 排行榜 / 会话缓存） |
| 数据库迁移 | Flyway（`flyway-core` + `flyway-mysql`） |
| 鉴权 | JJWT 0.12.5（统一 Bearer JWT） |
| API 文档 | SpringDoc OpenAPI 2.3.0（Swagger UI） |
| 可观测性 | Spring Boot Actuator + Micrometer（Tracing / Prometheus）+ Zipkin |
| 测试 | JUnit 5 + Mockito + H2（内存数据库，用于集成测试） |
| 构建 | Maven 3.9 + Spring Boot Plugin |
| 容器化 | Docker（Eclipse Temurin 17 JRE + G1GC） |

## 3. 端口号

| 端口 | 用途 |
|------|------|
| `8088` | HTTP / API |
| `8088` | WebSocket（`/ws/game`、`/ws/game/session`） |
| `8088` | Actuator（健康/指标） |

## 4. 启动方式

### 本地开发

```bash
# 前置：MySQL、Redis、Nacos 已启动
cd langtou-backend
mvn -pl langtou-common,langtou-game-service -am spring-boot:run
```

或直接运行主类：`com.langtou.game.GameServiceApplication`。

服务启动后 Flyway 会自动执行 `src/main/resources/db/migration/` 下的 V19 初始化脚本。

### Docker

```bash
cd langtou-backend
mvn -pl langtou-common,langtou-game-service -am clean package -DskipTests
docker build -t langtou-game-service ./langtou-game-service
docker run -d --name langtou-game-service \
  -p 8088:8088 \
  -e DB_PASSWORD=root \
  -e NACOS_SERVER=localhost:8848 \
  langtou-game-service
```

## 5. 依赖服务

本服务未通过 Feign 直接调用其他微服务，采用**独立部署**模式。
接入 Nacos 进行服务注册与发现，依赖 Redis 进行对局状态与排行榜缓存。

## 6. API 端点清单

### 对局（`GameSessionController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/game/sessions` | 创建对局 |
| GET | `/api/v1/game/sessions/{sessionId}` | 查询对局详情 |
| POST | `/api/v1/game/sessions/{sessionId}/join` | 加入对局 |
| POST | `/api/v1/game/sessions/{sessionId}/leave` | 离开对局 |
| POST | `/api/v1/game/sessions/{sessionId}/start` | 开始对局 |
| POST | `/api/v1/game/sessions/{sessionId}/end` | 结束对局 |

### 匹配（`GameMatchmakingController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/game/matchmaking/submit` | 提交匹配请求 |
| POST | `/api/v1/game/matchmaking/{matchmakingId}/cancel` | 取消匹配 |
| GET | `/api/v1/game/matchmaking/mmr` | 查询当前 MMR |

### 背包（`GameInventoryController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/game/inventories` | 查询我的背包 |
| POST | `/api/v1/game/inventories/{inventoryId}/use` | 使用道具 |
| POST | `/api/v1/game/inventories/{inventoryId}/equip` | 装备/卸下道具 |

### 排行榜（`GameLeaderboardController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/game/leaderboards` | 查询排行榜（`gameId`/`seasonId`/`limit`） |
| GET | `/api/v1/game/leaderboards/mine` | 查询我的排名 |
| POST | `/api/v1/game/leaderboards/update` | 更新我的得分排名 |

### 任务与成就（`GameQuestController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/game/quests` | 查询游戏任务列表 |
| POST | `/api/v1/game/quests/{questId}/claim` | 领取/完成任务奖励 |
| POST | `/api/v1/game/quests/progress` | 上报任务进度 |
| POST | `/api/v1/game/quests/{questId}/complete` | 完成任务 |

### 游戏支付（`GamePaymentController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/api/v1/game/payments/orders` | 创建订单 |
| GET | `/api/v1/game/payments/orders/{orderNo}` | 查询订单 |
| POST | `/api/v1/game/payments/callback` | 支付回调 |
| POST | `/api/v1/game/payments/orders/{orderNo}/refund` | 退款 |

### WebSocket 端点

| 端点 | 协议 | 功能 |
|------|------|------|
| `/ws/game` | STOMP over SockJS | 全局游戏通知通道 |
| `/ws/game/session` | STOMP over SockJS | 对局房间实时通道 |

订阅主题示例：`/topic/game/{gameId}`、`/queue/session/{sessionId}`。

> 所有 HTTP 接口需要在请求头携带 Bearer JWT。

## 7. 配置说明（`application.yml` 关键项）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8088` | 服务端口 |
| `spring.application.name` | `langtou-game-service` | 注册到 Nacos 的服务名 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/langtou` | MySQL 连接 |
| `spring.datasource.password` | `${DB_PASSWORD:root}` | 数据库密码 |
| `spring.redis.host` / `port` | `localhost:6379` | Redis 地址 |
| `spring.flyway.enabled` | `true` | 启动时自动迁移 |
| `spring.flyway.locations` | `classpath:db/migration` | Flyway 脚本目录 |
| `spring.flyway.baseline-on-migrate` | `true` | 基线版本 |
| `spring.cloud.nacos.discovery.server-addr` | `${NACOS_SERVER:localhost:8848}` | Nacos 地址 |
| `mybatis-plus.type-aliases-package` | `com.langtou.game.entity` | MyBatis-Plus 实体包 |
| `slow-query.enabled` / `threshold` | `true` / `500` | 慢查询监控（ms） |
| `management.zipkin.tracing.endpoint` | `http://localhost:9411` | Zipkin 上报地址 |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | 暴露的 Actuator 端点 |

> 测试环境配置位于 `src/test/resources/application.yml`，使用 H2 内存库 + 禁用 Redis/Nacos/Flyway。

## 8. 数据库表清单

本服务通过 Flyway 管理（`V19__game_service_schema.sql`），核心表如下：

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `GameSession` | `game_session` | 对局房间（状态：WAITING/IN_PROGRESS/FINISHED/CANCELLED） |
| `GameSessionPlayer` | `game_session_players` | 对局玩家关联 |
| `GameItem` | `game_item` | 游戏道具配置（类型/稀有度/叠加） |
| `GameInventory` | `game_inventory` | 玩家背包 |
| `GameMatchmaking` | `game_matchmaking` | 匹配记录（含 MMR、队列类型） |
| `GameLeaderboard` | `game_leaderboard` | 排行榜（按赛季唯一键） |
| `GameSeason` | `game_season` | 赛季配置 |
| `GameQuest` | `game_quest` | 任务/成就（DAILY/WEEKLY/ACHIEVEMENT） |
| `GamePayment` | `game_payment` | 游戏内支付订单 |

## 9. 测试命令

```bash
# 单元测试（JUnit 5 + Mockito）
mvn -pl langtou-game-service test

# 集成测试（H2 内存库 + @SpringBootTest）
mvn -pl langtou-game-service verify

# 打包跳过测试
mvn -pl langtou-game-service -am clean package -DskipTests
```

测试类说明：

- `GameSessionServiceImplTest`：对局服务单元测试（Mock Mapper / Redis）
- `GamePaymentServiceImplTest`：支付服务单元测试
- `GameSessionControllerIntegrationTest`：对局 API 集成测试（H2 + MockMvc）

## 10. Swagger UI 访问地址

- 本地：`http://localhost:8088/swagger-ui/index.html`
- API JSON：`http://localhost:8088/v3/api-docs`

## 附：健康检查

```bash
curl http://localhost:8088/actuator/health
curl http://localhost:8088/actuator/prometheus
```
