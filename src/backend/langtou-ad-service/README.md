# langtou-ad-service

榔头（Langtou）平台**广告服务**，负责广告投放、曝光/点击上报、素材管理以及推荐位管理。

## 1. 服务简介

为榔头平台提供广告系统的核心能力：广告素材/投放管理（管理员后台）、Feed/开屏广告获取、曝光/点击埋点上报，以及推荐位（广告/运营位）的配置与查询。

## 2. 技术栈

| 类别 | 技术 |
|------|------|
| 语言 / 框架 | Java 17 + Spring Boot 3.2.5 |
| 微服务治理 | Spring Cloud 2023.0.1 + Nacos Discovery |
| ORM | MyBatis-Plus 3.5.6 |
| 数据库 | MySQL 8.0（数据库：`langtou`） |
| 缓存 | Redis（Lettuce 客户端） |
| 鉴权 | JJWT 0.12.5 + `@RequireRole`（支持 `ADMIN` 角色） |
| API 文档 | SpringDoc OpenAPI 2.3.0（Swagger UI） |
| 可观测性 | Spring Boot Actuator + Micrometer（Tracing / Prometheus）+ Zipkin |
| 构建 | Maven 3.9 + Spring Boot Plugin |
| 容器化 | Docker（Eclipse Temurin 17 JRE + G1GC） |

## 3. 端口号

| 端口 | 用途 |
|------|------|
| `8086` | HTTP / API |
| `8086` | Actuator（健康/指标） |

## 4. 启动方式

### 本地开发

```bash
# 前置：MySQL、Redis、Nacos 已启动
cd langtou-backend
mvn -pl langtou-common,langtou-ad-service -am spring-boot:run
```

或直接运行主类：`com.langtou.ad.AdServiceApplication`。

### Docker

```bash
cd langtou-backend
mvn -pl langtou-common,langtou-ad-service -am clean package -DskipTests
docker build -t langtou-ad-service ./langtou-ad-service
docker run -d --name langtou-ad-service \
  -p 8086:8086 \
  -e DB_PASSWORD=root \
  -e NACOS_SERVER=localhost:8848 \
  langtou-ad-service
```

## 5. 依赖服务

本服务通过 `@RequireRole` 注解完成 JWT 角色校验，未直接使用 Feign 调用其他微服务；接入 Nacos 进行服务注册与发现。

## 6. API 端点清单

### 对外广告接口（`AdController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/ads/feed` | 获取 Feed 流广告（`count`） |
| GET | `/api/v1/ads/splash` | 获取开屏广告 |
| POST | `/api/v1/ads/{adId}/impression` | 上报广告曝光 |
| POST | `/api/v1/ads/{adId}/click` | 上报广告点击 |

### 管理员广告接口（`AdminAdController`，需 `ADMIN` 角色）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/admin/ads` | 广告列表（支持 `adType`/`status`/`keyword` 筛选） |
| POST | `/api/v1/admin/ads` | 创建广告 |
| PUT | `/api/v1/admin/ads/{adId}` | 更新广告信息 |
| DELETE | `/api/v1/admin/ads/{adId}` | 删除广告 |
| PUT | `/api/v1/admin/ads/{adId}/status` | 上下架广告 |
| GET | `/api/v1/admin/ads/{adId}/stats` | 广告效果统计 |

### 推荐位接口（`RecommendPositionController`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/v1/recommend-positions/{type}` | 按类型查询启用的推荐位 |
| GET | `/api/v1/recommend-positions/admin` | 推荐位列表（管理员） |
| POST | `/api/v1/recommend-positions/admin` | 创建推荐位（管理员） |
| PUT | `/api/v1/recommend-positions/admin/{id}` | 更新推荐位（管理员） |
| DELETE | `/api/v1/recommend-positions/admin/{id}` | 删除推荐位（管理员） |

> 对外广告接口需要在请求头携带 JWT（或 `X-User-Id`）。

## 7. 配置说明（`application.yml` 关键项）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8086` | 服务端口 |
| `spring.application.name` | `langtou-ad-service` | 注册到 Nacos 的服务名 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/langtou` | MySQL 连接 |
| `spring.datasource.password` | `${DB_PASSWORD:root}` | 数据库密码，支持环境变量 |
| `spring.redis.host` / `port` | `localhost:6379` | Redis 地址 |
| `spring.cloud.nacos.discovery.server-addr` | `${NACOS_SERVER:localhost:8848}` | Nacos 地址 |
| `mybatis-plus.type-aliases-package` | `com.langtou.ad.entity` | MyBatis-Plus 实体包 |
| `slow-query.enabled` / `threshold` | `true` / `500` | 慢查询监控（ms） |
| `management.zipkin.tracing.endpoint` | `http://localhost:9411` | Zipkin 上报地址 |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` | 暴露的 Actuator 端点 |

## 8. 数据库表清单

本服务共享 `langtou` 数据库，核心表如下：

| 实体类 | 表名 | 说明 |
|--------|------|------|
| `Advertisement` | `advertisement` | 广告主表（标题、素材、类型、状态、曝光/点击统计） |
| `AdImpression` | `ad_impression` | 广告曝光埋点记录 |
| `AdClick` | `ad_click` | 广告点击埋点记录 |
| `RecommendPosition` | `recommend_position` | 推荐位（广告位/运营位）配置 |

## 9. 测试命令

```bash
# 单元测试
mvn -pl langtou-ad-service test

# 打包跳过测试
mvn -pl langtou-ad-service -am clean package -DskipTests
```

> 当前 `src/test` 目录尚未包含测试类，测试体系将在后续 Sprint 中补齐。

## 10. Swagger UI 访问地址

- 本地：`http://localhost:8086/swagger-ui/index.html`
- API JSON：`http://localhost:8086/v3/api-docs`

## 附：健康检查

```bash
curl http://localhost:8086/actuator/health
curl http://localhost:8086/actuator/prometheus
```
