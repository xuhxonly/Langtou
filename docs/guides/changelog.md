# Changelog

榔头（Langtou）项目版本变更记录，遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 规范，
版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [v7.1.0] - 2026-06-18

### 新增（Added）

- 新增 **创作者服务（langtou-creator-service）**（端口 8085）
  - 创作者仪表盘与数据分析：概览、趋势、笔记排行、粉丝画像、流量漏斗、内容诊断
  - 佣金管理：佣金列表、汇总、趋势、提现申请
  - 广告收益：收益总览、明细、趋势
  - 钱包管理：钱包查询/创建、提现
  - 核心表：`creator_wallet`、`creator_commission`、`creator_ad_revenue`、`creator_daily_stats`、`withdrawal_request`、`user_profile`
- 新增 **广告服务（langtou-ad-service）**（端口 8086）
  - 对外广告接口：Feed 广告、开屏广告、曝光/点击上报
  - 管理员广告后台：CRUD、上下架、效果统计
  - 推荐位管理：按类型查询、管理员 CRUD
  - 核心表：`advertisement`、`ad_impression`、`ad_click`、`recommend_position`
- 新增 **AI 创作服务（langtou-ai-service）**（端口 8087）
  - AI 写作助手：标题生成、标签推荐、草稿生成、封面推荐
  - 基于 Spring AI 1.0.0-M5，默认 `mock` Provider，生产可切换至阿里云 DashScope（`qwen-turbo`）
  - 单用户限流 + Redis 结果缓存
- 新增 **游戏化服务（langtou-game-service）**（端口 8088）
  - 实时对局：创建/加入/离开/开始/结束（STOMP over WebSocket `/ws/game`、`/ws/game/session`）
  - 匹配系统：MMR 匹配、提交/取消匹配
  - 玩家背包：道具查询、使用、装备
  - 排行榜：按游戏/赛季查询、我的排名、得分更新
  - 任务与成就：领取、进度上报、完成
  - 游戏支付：订单创建、查询、回调、退款
  - Flyway 初始化脚本：`V19__game_service_schema.sql`
  - 测试：JUnit 5 + Mockito + H2 集成测试

### 变更（Changed）

- 根 `pom.xml` 模块列表新增 `langtou-creator-service`、`langtou-ai-service`、`langtou-ad-service`、`langtou-game-service`
- 项目版本号升级至 `7.0.0-RELEASE`（与根 POM 保持一致）

### 文档（Docs）

- 补齐 4 个新微服务的 `README.md`，涵盖：服务简介、技术栈、端口号、启动方式（本地 + Docker）、依赖服务、API 端点清单、配置说明、数据库表清单、测试命令、Swagger UI 地址

### 兼容性

- 本次新增为**向后兼容**的功能扩展，未修改既有模块对外 API
- 数据库复用现有 `langtou` schema，通过 MyBatis-Plus 实体映射或 Flyway 迁移脚本管理
- 所有服务统一接入 Nacos Discovery（可通过 `NACOS_SERVER` 环境变量覆盖）

---

## [v7.0.0] - 2026-06-12

### 新增（Added）

- 完成 Sprint 1-4 全部微服务搭建：
  - 网关 `langtou-gateway`（8080）
  - 用户服务 `langtou-user-service`（8081）
  - 内容服务 `langtou-content-service`（8082）
  - 互动服务 `langtou-interact-service`（8083）
  - 消息服务 `langtou-message-service`（8084）
  - 公共模块 `langtou-common`（Result / JWT / 异常 / 拦截器 / 监控）
- 技术栈：Spring Boot 3.2.5 + Spring Cloud 2023.0.1 + MyBatis-Plus 3.5.6 + MySQL 8 + Redis
- 引入 SpringDoc OpenAPI、Micrometer Tracing、Zipkin、Prometheus 全套可观测性
- 统一鉴权：JWT + `@RequireRole` 注解

[v7.1.0]: https://git.langtou.com/langtou/langtou/compare/v7.0.0...v7.1.0
[v7.0.0]: https://git.langtou.com/langtou/langtou/releases/tag/v7.0.0
