# langtou-creator-service

榔头（Langtou）平台**创作者服务**，负责创作者侧的钱包、收益（广告分成/佣金）、数据分析与提现能力。

## 1. 服务简介

面向创作者的商业化与数据中心服务，提供创作者仪表盘、内容分析、佣金明细、广告收益、钱包管理与提现申请等核心能力。

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 17 + Spring Boot 3.2.5 |
| 微服务治理 | Spring Cloud 2023.0.1 + Nacos Discovery |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8.0（数据库：`langtou`） |
| 缓存 | Redis（Lettuce 客户端） |
| 鉴权 | JJWT 0.12.5（JWT）+ 自定义 `@RequireRole` 注解 |
| API 文档 | SpringDoc OpenAPI 2.3.0（Swagger UI） |
| 可观测性 | Spring Boot Actuator + Micrometer（Tracing / Prometheus）+ Zipkin |
| 构建 | Maven 3.9（多模块）+ Spring Boot Plugin |
| 容器化 | Docker（Eclipse Temurin 17 JRE + G1GC） |

## 3. 端口号

| 端口 | 用途 |
|------|------|
| `8085` | HTTP / API |
| `8085` | Actuator（健康/指标） |

## 4. 启动方式

### 本地开发

```bash
# 前置：MySQL、Redis、Nacos 已启动
cd langtou-backend
mvn -pl langtou-common,langtou-creator-service -am spring-boot:run
```

或直接运行主类：`com.langtou.creator.CreatorServiceApplication`。

### Docker

```bash
cd langtou-backend
mvn -pl langtou-common,langtou-creator-service -am clean package -DskipTests
docker build -t langtou-creator-service ./langtou-creator-service
docker run -d --name langtou-creator-service \
  -p 8085:8085 \
  -e DB_PASSWORD=root \
  -e NACOS_SERVER=localhost:8848 \
  langtou-creator-service
```

## 5. 依赖服务

本服务通过 `langtou-common` 的 `@RequireRole` 注解完成鉴权拦截，未直接使用 Feign 远程调用其他服务（数据由自身表提供）。
接入 Nacos 进行服务注册与发现。

## 6. API 端点清单

### 创作者仪表盘与分析（`CreatorAnalyticsController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/creator/dashboard` | 获取创作者仪表盘概览 |
| GET | `/api/v1/creator/dashboard/trend` | 获取趋势数据（`days`） |
| GET | `/api/v1/creator/dashboard/note-ranking` | 笔记排行 Top N |
| GET | `/api/v1/creator/dashboard/fan-profile` | 粉丝画像 |
| GET | `/api/v1/creator/analytics/content/{contentId}` | 单条内容分析 |
| GET | `/api/v1/creator/analytics/funnel/{contentId}` | 内容流量漏斗 |
| GET | `/api/v1/creator/analytics/traffic-sources` | 流量来源分布 |
| GET | `/api/v1/creator/analytics/diagnosis` | 内容诊断报告 |
| GET | `/api/v1/creator/analytics/daily-stats` | 区间日度数据（`startDate`/`endDate`） |

### 创作者佣金（`CommissionController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/creator/commissions` | 佣金列表（分页） |
| GET | `/api/v1/creator/commissions/summary` | 佣金汇总 |
| GET | `/api/v1/creator/commissions/trend` | 佣金趋势（`period`） |
| POST | `/api/v1/creator/commissions/withdraw` | 申请提现 |

### 创作者收益（`CreatorRevenueController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/creator/revenue/overview` | 收益总览 |
| GET | `/api/v1/creator/revenue/details` | 广告收益明细（分页） |
| GET | `/api/v1/creator/revenue/trend` | 收益趋势 |

### 创作者钱包（`WalletController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/creator/wallet` | 查询/创建钱包 |
| POST | `/api/v1/creator/wallet/withdraw` | 钱包发起提现 |

> 所有接口需要在请求头携带 `X-User-Id`（创作者用户 ID）。

## 7. 配置说明（`application.yml` 关键项）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8085` | 服务端口 |
| `spring.application.name` | `langtou-creator-service` | 注册到 Nacos 的服务名 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/langtou` | MySQL 连接 |
| `spring.datasource.password` | `${DB_PASSWORD:root}` | 数据库密码，支持环境变量 |
| `spring.redis.host` / `port` | `localhost:6379` | Redis 地址 |
| `spring.cloud.nacos.discovery.server-addr` | `${NACOS_SERVER:localhost:8848}` | Nacos 地址 |
| `mybatis-plus.type-aliases-package` | `com.langtou.creator.entity` | MyBatis-Plus 实体包 |
| `slow-query.enabled` / `threshold` | `true` / `500` | 慢查询监控（ms） |
| `management.zipkin.tracing.endpoint` | `http://localhost:9411` | Zipkin 上报地址 |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | 暴露的 Actuator 端点 |

## 8. 数据库表清单

本服务共享 `langtou` 数据库，核心表如下（MyBatis-Plus 实体）：

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `CreatorWallet` | `creator_wallet` | 创作者钱包（总额/可用/待结算/已提现） |
| `CreatorCommission` | `creator_commission` | 创作者佣金（电商/带货类分成） |
| `CreatorAdRevenue` | `creator_ad_revenue` | 创作者广告分成收益明细 |
| `CreatorDailyStats` | `creator_daily_stats` | 创作者日度统计（曝光/互动/粉丝） |
| `WithdrawalRequest` | `withdrawal_request` | 提现申请单 |
| `UserProfile` | `user_profile` | 创作者扩展资料 |

## 9. 测试命令

```bash
# 单元测试（JUnit 5）
mvn -pl langtou-creator-service test

# 打包跳过测试
mvn -pl langtou-creator-service -am clean package -DskipTests
```

## 10. Swagger UI 访问地址

- 本地：`http://localhost:8085/swagger-ui/index.html`
- API JSON：`http://localhost:8085/v3/api-docs`
- OpenAPI 分组文档（由 `OpenApiGroupConfig` 定义）

## 附：健康检查

```bash
curl http://localhost:8085/actuator/health
curl http://localhost:8085/actuator/prometheus   # Prometheus