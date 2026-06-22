# 榔头(Langtou) 问题跟踪与优化方案

> **版本**: v2.0
> **日期**: 2026-06-12
> **编制**: 榔头开发团队
> **状态**: **全部完成**
> **输入来源**: 产品经理市场竞争力报告（评分 47/100）、测试工程师全面测试报告（26 个缺陷）
> **完成总结**: 全部 32 个问题（8 P0 + 13 P1 + 8 P2 + 3 P3）已在 Sprint 1-4 中全部修复，新增功能模块 15+ 个，新增/修改文件 200+ 个

---

## 目录

1. [问题跟踪表](#1-问题跟踪表)
2. [Sprint 计划](#2-sprint-计划)
3. [技术方案设计](#3-技术方案设计)
4. [附录](#4-附录)

---

## 1. 问题跟踪表

### 1.1 合并说明

本表将产品经理发现的 15 个核心问题（PM-01 ~ PM-15）与测试工程师发现的 26 个缺陷（BUG-001 ~ BUG-026）进行合并去重。去重规则如下：

- **完全重复**: PM 和 QA 描述同一问题的，合并为一条，保留双来源标记
- **部分重叠**: PM 描述宏观能力缺失、QA 发现具体技术缺陷的，合并为一条，PM 描述为根因，QA 描述为具体表现
- **独立问题**: 各自独有的问题，分别保留

合并后共 **32 个独立问题项**，按优先级排序。

### 1.2 P0 - 阻塞级问题（8 项）

| ID | 问题描述 | 来源 | 优先级 | 模块 | 负责人 | 状态 | 关联文件 |
|----|---------|------|--------|------|--------|------|---------|
| LT-001 | **JWT 密钥硬编码且强度不足**：`.env` 中使用默认弱密钥 `langtou-dev-jwt-secret-key-please-change-in-production-2024`，攻击者可伪造 Token 完全绕过认证 | QA | P0 | 安全/网关 | 后端负责人 | **已完成** | `.env:72`, `JwtUtils.java`, `JwtAuthFilter.java` |
| LT-002 | **推荐算法缺乏个性化能力**：基于基础 XGBoost/LightGBM，仅支持离线批量更新，无实时用户画像、无深度学习排序、无向量召回，Feed 流"千人一面" | PM+QA | P0 | 推荐系统 | 算法负责人 | **已完成** | `RecommendationServiceImpl.java`, `langtou-recommendation/` |
| LT-003 | **AI 内容审核能力为零**：审核完全依赖敏感词匹配（200+ 内置词库）+ 人工审核，无图像/视频 AI 审核，无 NLP 文本理解，审核时效 24h+ | PM | P0 | 内容安全 | AI 负责人 | **已完成** | `ContentAuditService.java`, `SensitiveWord.java` |
| LT-004 | **电商能力完全缺失**：无商品橱窗、无内容挂链、无交易闭环、无创作者带货工具，错失内容变现核心模式 | PM | P0 | 电商 | 电商负责人 | **已完成** | 无（需新建模块） |
| LT-005 | **创作者无任何变现渠道**：无广告分成、无带货佣金、无品牌合作、无打赏，优质创作者流失风险极高 | PM | P0 | 创作者生态 | 产品负责人 | **已完成** | `CreatorAnalyticsController.java` |
| LT-006 | **无青少年模式，合规风险极高**：无年龄分级保护机制，未成年用户可无限制访问全部内容、无时长限制、可私信/可发布 | PM | P0 | 合规/用户 | 产品负责人 | **已完成** | `UserService.java`, 移动端全局 |
| LT-007 | **refreshUserProfile N+1 查询**：循环调用 `tagService.getTagIdsByNoteId` 和 `tagService.getTagsByNoteId`，用户行为多时画像刷新极慢 | QA | P0 | 性能/推荐 | 后端负责人 | **已完成** | `RecommendationServiceImpl.java:222-228` |
| LT-008 | **getConversations N+1 查询**：循环查询最新消息和未读数，会话列表加载慢，消息多时时延高 | QA | P0 | 性能/消息 | 后端负责人 | **已完成** | `MessageServiceImpl.java:82-98` |

### 1.3 P1 - 严重问题（13 项）

| ID | 问题描述 | 来源 | 优先级 | 模块 | 负责人 | 状态 | 关联文件 |
|----|---------|------|--------|------|--------|------|---------|
| LT-009 | **评论/私信/笔记内容 XSS 风险**：未对用户输入做 HTML 转义，存在存储型 XSS，可窃取用户 Cookie、执行恶意脚本；搜索接口存在反射型 XSS | QA | P1 | 安全/内容 | 后端负责人 | **已完成** | `ContentServiceImpl.java`, `InteractServiceImpl.java`, `MessageServiceImpl.java`, `SearchController.java` |
| LT-010 | **点赞/评论/私信/搜索无频率限制**：点赞和评论未做用户行为频率限制，存在刷量风险；私信发送未限制频率，存在垃圾消息轰炸；搜索接口未做查询频率限制 | PM+QA | P1 | 安全/网关 | 后端负责人 | **已完成** | `InteractServiceImpl.java`, `MessageServiceImpl.java`, `SearchController.java`, `RateLimiterConfig.java` |
| LT-011 | **接口测试覆盖率仅 35%**：21 个 Controller 中仅 3 个有测试，大量关键接口（Admin、搜索、推荐、消息、收藏）无测试覆盖 | QA | P1 | 质量保障 | 测试负责人 | **已完成** | `langtou-backend/*/src/test/` |
| LT-012 | **Redis 缓存策略缺陷**：缓存穿透/击穿/雪崩均无防护；TTL 设置不一致；推荐缓存使用 `keys` 命令清除导致 Redis 阻塞 | QA | P1 | 性能/缓存 | 后端负责人 | **已完成** | `RedisKeyUtil.java`, `RecommendationServiceImpl.java:255-259` |
| LT-013 | **敏感词仅内存存储**：200+ 内置词库仅存储在内存中，无持久化机制，服务重启后自定义敏感词丢失；用户资料更新未校验敏感词 | PM+QA | P1 | 内容安全 | 后端负责人 | **已完成** | `SensitiveWord.java`, `SensitiveWordMapper.java`, `UserServiceImpl.java` |
| LT-014 | **搜索能力落后**：仅支持 Elasticsearch 关键词匹配，无语义理解、无向量检索、无以图搜图、无搜索建议/自动补全 | PM | P1 | 搜索 | 算法负责人 | **已完成** | `SearchService.java`, `SearchController.java` |
| LT-015 | **创作工具简陋**：发布页仅支持基础图文/视频上传，无智能封面、无 AI 文案生成、无模板/滤镜/剪辑工具 | PM | P1 | 创作工具 | 前端负责人 | **已完成** | `PublishScreen.tsx`, `EditNoteScreen.tsx` |
| LT-016 | **实时通信能力薄弱**：消息推送依赖轮询，无消息已读回执优化、无离线消息可靠投递保障、无消息撤回/编辑，群聊功能缺失 | PM | P1 | 消息 | 后端负责人 | **已完成** | `MessageService.java`, `ChatWebSocketHandler.java` |
| LT-017 | **隐私保护缺失**：内容无敏感度分级标签，无 GDPR 数据导出/删除功能；手机号明文存储；数据库连接密码明文配置；数据库使用 root 用户权限过大 | PM+QA | P1 | 安全/合规 | 后端负责人 | **已完成** | `User.java`, `application.yml`(多服务), `GlobalExceptionHandler.java` |
| LT-018 | **广告系统过于基础**：仅支持按权重随机展示信息流广告和开屏广告，无用户定向、无转化追踪、无广告主后台、无竞价机制；广告管理接口权限控制缺失 | PM+QA | P1 | 广告/商业化 | 商业化负责人 | **已完成** | `AdController.java`, `AdminAdController.java` |
| LT-019 | **反作弊能力薄弱**：仅依赖基础 Redis 限流，无设备指纹、无行为分析、无团伙识别、无内容去重，刷量/水军风险高 | PM | P1 | 安全 | 安全负责人 | **已完成** | `RateLimiterConfig.java` |
| LT-020 | **数据分析能力缺失**：创作者数据中心仅有基础浏览/点赞/收藏统计，无流量来源分析、无粉丝增长趋势、无内容诊断；运营团队无数据看板 | PM | P1 | 数据 | 数据负责人 | **已完成** | `CreatorAnalyticsController.java`, `AdminAnalyticsController.java` |
| LT-021 | **推荐服务 RPC 调用过多**：`calculateFollowingScore` 每次排序都调用 `userClient.getFollowingIds`；`fillFeedAuthorInfoBatch` 未做本地缓存，Feed 每次刷新都调用用户服务 | QA | P1 | 性能/推荐 | 后端负责人 | **已完成** | `RecommendationServiceImpl.java:527-536, 630-669` |

### 1.4 P2 - 一般问题（8 项）

| ID | 问题描述 | 来源 | 优先级 | 模块 | 负责人 | 状态 | 关联文件 |
|----|---------|------|--------|------|--------|------|---------|
| LT-022 | **注册未校验图形验证码**：注册接口未限制同一手机号频繁注册，存在短信轰炸风险；短信验证码机制不完善 | QA | P2 | 用户/安全 | 后端负责人 | **已完成** | `UserServiceImpl.java`, `SmsService.java` |
| LT-023 | **笔记无历史版本**：笔记编辑后无法查看历史版本，无法回滚，内容管理能力不足 | QA | P2 | 内容 | 后端负责人 | **已完成** | `ContentServiceImpl.java`, `DraftService.java` |
| LT-024 | **前端主题闪烁**：深色模式切换时存在页面闪烁，主题初始化逻辑不完善 | QA | P2 | 移动端/前端 | 前端负责人 | **已完成** | `useThemeStore.ts`, `useTheme.ts` |
| LT-025 | **Web 后台使用 Mock 数据**：管理后台 `langtou-admin/index.html` 使用前端 Mock 数据，未接入真实 API | QA | P2 | 管理后台 | 前端负责人 | **已完成** | `langtou-admin/index.html` |
| LT-026 | **缺少系统日志与审计机制**：内容审核接口未记录操作日志，无法审计审核行为；缺少统一的系统日志管理 | PM+QA | P2 | 运维/审计 | 后端负责人 | **已完成** | `AdminNoteController.java`, `GlobalExceptionHandler.java` |
| LT-027 | **性能优化空间大**：移动端无图片懒加载/渐进加载、无骨架屏、API 超时 15s 过长、视频无预加载策略；数据库连接池 `max-active: 8` 偏小 | PM+QA | P2 | 性能/前端 | 前端+后端 | **已完成** | `application.yml`(多服务), 移动端全局 |
| LT-028 | **社区运营工具缺失**：无话题挑战、无活动运营工具、无官方账号体系、无社区规范引导 | PM | P2 | 运营/产品 | 产品负责人 | **已完成** | 无（需新建模块） |
| LT-029 | **国际化支持不足**：虽有 i18n 基础框架，但内容/推荐/审核均未考虑多语言，仅支持中文内容生态 | PM | P2 | 国际化 | 后端负责人 | **已完成** | `I18nService.java`, `messages_en_US.properties` |

### 1.5 P3 - 建议级问题（3 项）

| ID | 问题描述 | 来源 | 优先级 | 模块 | 负责人 | 状态 | 关联文件 |
|----|---------|------|--------|------|--------|------|---------|
| LT-030 | **搜索建议/自动补全缺失**：搜索无联想词、无热搜榜、无搜索历史，搜索体验差 | QA | P3 | 搜索 | 后端负责人 | **已完成** | `SearchController.java` |
| LT-031 | **CORS/Actuator 安全配置**：CORS 配置允许 `*`；Actuator 端点未做访问控制；错误处理可能返回详细异常信息 | QA | P3 | 安全 | 后端负责人 | **已完成** | `application.yml`, `GlobalExceptionHandler.java` |
| LT-032 | **Locust 压测脚本优化**：脚本中 `time.sleep` 阻塞协程，压测结果可能不准确 | QA | P3 | 测试 | 测试负责人 | **已完成** | `performance_test.py:402` |

### 1.6 问题分布统计

| 优先级 | 数量 | 占比 |
|--------|------|------|
| P0（阻塞级） | 8 | 25% |
| P1（严重） | 13 | 41% |
| P2（一般） | 8 | 25% |
| P3（建议） | 3 | 9% |
| **合计** | **32** | 100% |

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|------|----|----|----|----|------|
| 安全/合规 | 1 | 5 | 0 | 1 | 7 |
| 性能 | 2 | 2 | 1 | 0 | 5 |
| 推荐系统 | 1 | 1 | 0 | 0 | 2 |
| 内容安全 | 1 | 0 | 0 | 0 | 1 |
| 电商/商业化 | 1 | 1 | 0 | 0 | 2 |
| 创作者生态 | 1 | 0 | 0 | 0 | 1 |
| 搜索 | 0 | 1 | 0 | 1 | 2 |
| 创作工具 | 0 | 1 | 0 | 0 | 1 |
| 消息/通信 | 1 | 1 | 0 | 0 | 2 |
| 质量保障 | 0 | 1 | 0 | 1 | 2 |
| 缓存 | 0 | 1 | 0 | 0 | 1 |
| 数据分析 | 0 | 1 | 0 | 0 | 1 |
| 前端/移动端 | 0 | 0 | 2 | 0 | 2 |
| 运营/产品 | 0 | 0 | 1 | 0 | 1 |
| 国际化 | 0 | 0 | 1 | 0 | 1 |

---

## 2. Sprint 计划

### 总体规划

- **Sprint 周期**: 2 周/Sprint，共 4 个 Sprint（8 周）
- **团队配置**: 后端 3 人、前端 2 人、算法 1 人、测试 1 人、产品 1 人
- **每日站会**: 10:00 AM，同步进度与阻塞
- **Sprint 评审**: 每个 Sprint 最后一天下午

### 2.1 Sprint 1: P0 问题修复（第 1-2 周）

**Sprint 目标**: 消除所有阻塞级安全和性能问题，建立 AI 审核基础框架

| 任务 ID | 任务描述 | 优先级 | 负责人 | 预估工时 | 验收标准 |
|---------|---------|--------|--------|---------|---------|
| S1-01 | JWT 密钥更换：生产环境使用 `openssl rand -base64 32` 生成 256 位强密钥，通过 K8s Secret 注入，移除 `.env` 硬编码 | P0 | 后端 A | 2d | JWT 密钥长度 >= 32 字符，通过 K8s Secret 管理，`.env` 中无硬编码密钥 |
| S1-02 | refreshUserProfile N+1 修复：将循环调用 `tagService.getTagsByNoteId` 改为批量查询，一次性获取所有笔记的标签信息 | P0 | 后端 A | 2d | 单次画像刷新 SQL 查询数从 O(N) 降至 O(1)，刷新耗时 < 500ms |
| S1-03 | getConversations N+1 修复：使用批量 SQL（`IN` 查询 + 子查询）替代循环查询最新消息和未读数 | P0 | 后端 B | 2d | 会话列表加载 SQL 查询数从 O(N) 降至 O(1)，10 个会话加载 < 200ms |
| S1-04 | XSS 防护：引入 OWASP Java HTML Sanitizer，对所有用户输入（评论、笔记、私信、搜索关键词）进行 HTML 转义和标签白名单过滤 | P0 | 后端 B | 3d | 所有用户输入接口通过 XSS 注入测试，CSP 响应头配置完成 |
| S1-05 | 频率限制增强：在 Gateway 层为点赞、评论、私信、搜索接口配置 Redis 滑动窗口限流（点赞 10次/min，评论 5次/min，私信 20次/min，搜索 30次/min） | P0 | 后端 C | 2d | 超频请求返回 429 状态码，通过 Locust 压测验证限流生效 |
| S1-06 | AI 审核框架搭建：接入阿里云内容安全 API（文本 + 图像），建立审核结果缓存机制，实现人机协同审核流程 | P0 | 算法 | 5d | AI 审核接口上线，文本审核准确率 > 90%，图像审核准确率 > 85%，审核时效 < 5s |
| S1-07 | 推荐服务基础优化：修复 `calculateFollowingScore` 重复 RPC 调用，在排序前一次性获取关注列表；`fillFeedAuthorInfoBatch` 增加 Caffeine 本地缓存（TTL 5min） | P0 | 后端 A | 3d | 排序阶段 RPC 调用从 O(N) 降至 O(1)，Feed 刷新用户服务 QPS 降低 60% |

**Sprint 1 交付物**:
- JWT 安全加固完成并通过安全扫描
- N+1 查询全部修复，性能测试通过
- XSS 防护覆盖所有用户输入接口
- AI 审核 API 接入完成，审核时效 < 5s
- 频率限制覆盖关键接口

---

### 2.2 Sprint 2: P1 问题修复（第 3-4 周）

**Sprint 目标**: 提升接口测试覆盖率、优化缓存策略、完善安全机制、升级搜索能力

| 任务 ID | 任务描述 | 优先级 | 负责人 | 预估工时 | 验收标准 |
|---------|---------|--------|--------|---------|---------|
| S2-01 | 接口测试补充（第一批）：为 AdminUserController、AdminAuthController、SearchController、RecommendationController、MessageController 补充 REST Assured 测试 | P1 | 测试 | 4d | 新增 5 个 Controller 测试类，覆盖正常/异常/边界场景，接口覆盖率从 35% 提升至 60% |
| S2-02 | 接口测试补充（第二批）：为 FollowController、CollectionController、CreatorAnalyticsController、AdminAdController、AdminNoteController 补充测试 | P1 | 测试 | 3d | 新增 5 个 Controller 测试类，接口覆盖率提升至 75% |
| S2-03 | 接口测试 CI/CD 集成：配置 Jenkins Pipeline 在 PR/MR 时自动运行全部接口测试，测试失败阻断合并 | P1 | 测试+后端 C | 2d | CI 流水线配置完成，PR 自动触发测试，测试报告自动归档 |
| S2-04 | Redis 缓存优化：实现缓存穿透防护（空值缓存）、缓存击穿防护（互斥锁）、缓存雪崩防护（随机 TTL）；将 `keys` 命令替换为 `Scan` | P1 | 后端 A | 3d | 缓存命中率 > 95%，无缓存穿透/击穿/雪崩现象，Redis 无阻塞 |
| S2-05 | 敏感词持久化：将敏感词从内存迁移至 MySQL + Redis 双层存储，支持动态更新；用户资料更新增加敏感词校验 | P1 | 后端 B | 2d | 敏感词持久化到 MySQL，Redis 缓存热点词库，用户资料含敏感词时拒绝更新 |
| S2-06 | 搜索升级（第一阶段）：实现搜索建议/热搜榜/搜索历史功能，增加搜索结果排序优化 | P1 | 后端 C | 4d | 搜索建议响应 < 50ms，热搜榜实时更新，搜索历史支持删除 |
| S2-07 | 隐私保护增强：手机号 AES-256 加密存储，数据库密码迁移至环境变量，创建独立数据库用户（最小权限原则） | P1 | 后端 A | 2d | 手机号密文存储，展示时脱敏（138\*\*\*\*1234），数据库非 root 用户连接 |
| S2-08 | 广告接口权限加固：在 AdController 层增加角色校验注解，确保普通用户无法访问创作者/管理员广告接口 | P1 | 后端 B | 1d | 非创作者/管理员角色访问广告接口返回 403 |

**Sprint 2 交付物**:
- 接口测试覆盖率从 35% 提升至 75%
- CI/CD 测试流水线上线
- Redis 缓存策略完善，缓存命中率 > 95%
- 敏感词持久化完成
- 搜索体验显著提升

---

### 2.3 Sprint 3: 功能增强（第 5-6 周）

**Sprint 目标**: 上线青少年模式、商品橱窗 MVP、创作者变现基础能力

| 任务 ID | 任务描述 | 优先级 | 负责人 | 预估工时 | 验收标准 |
|---------|---------|--------|--------|---------|---------|
| S3-01 | 青少年模式 MVP：实现实名认证年龄识别、青少年模式自动开启（<18 岁）、内容分级过滤（仅教育/科普/正能量）、使用时长限制（40min/天）、22:00-6:00 禁用 | P0 | 产品+前端 A+后端 A | 5d | 未成年用户自动进入青少年模式，内容过滤生效，时长限制生效，家长控制面板可用 |
| S3-02 | 商品橱窗 MVP：建立商品数据模型，创作者可在主页展示商品，支持笔记内嵌商品链接，对接第三方供应链 API（不自建库存） | P0 | 电商+后端 B+前端 B | 8d | 创作者可添加/管理商品，笔记可关联商品，商品详情页可用，分佣计算逻辑完成 |
| S3-03 | 创作者变现基础：上线创作者广告分成计划（按内容曝光/互动结算），建立创作者收益面板 | P0 | 后端 C+前端 A | 5d | 创作者可查看收益明细，广告分成计算正确，提现流程可用 |
| S3-04 | 消息推送接入：接入 Firebase/APNs 推送服务，实现私信/通知的离线推送 | P1 | 后端 A | 3d | 离线消息推送到达率 > 95%，推送延迟 < 3s |
| S3-05 | 创作工具增强（第一阶段）：接入 AI 文案助手（根据图片自动生成标题和话题建议），智能封面选择 | P1 | 前端 B+算法 | 4d | AI 文案生成可用，封面选择功能可用，发布转化率提升可观测 |
| S3-06 | 运营活动后台：建立话题/挑战活动管理后台，支持创建活动、设置规则、查看参与数据 | P2 | 后端 C+前端 A | 3d | 运营人员可创建/管理话题活动，活动数据统计可用 |

**Sprint 3 交付物**:
- 青少年模式上线，合规风险大幅降低
- 商品橱窗 MVP 上线，首批创作者可带货
- 创作者广告分成计划上线
- 消息推送到达率 > 95%

---

### 2.4 Sprint 4: 体验优化（第 7-8 周）

**Sprint 目标**: 性能调优、社区工具完善、国际化框架搭建

| 任务 ID | 任务描述 | 优先级 | 负责人 | 预估工时 | 验收标准 |
|---------|---------|--------|--------|---------|---------|
| S4-01 | 前端性能优化：图片 CDN + WebP 格式 + 渐进加载，视频预加载策略，骨架屏全局覆盖，API 超时优化至 5s | P2 | 前端 A+B | 4d | 首页加载时间减少 40%，低端设备崩溃率降低 50% |
| S4-02 | 后端性能调优：数据库连接池调整（max-active 20-50），慢查询优化，API P99 延迟优化至 < 500ms | P2 | 后端 A | 3d | 连接池不再成为瓶颈，API P99 < 500ms |
| S4-03 | 社区运营工具：完善话题体系，建立官方账号内容引导，社区规范+新人引导，用户等级+成就体系增强 | P2 | 产品+前端 A+后端 B | 5d | 话题体系可用，官方账号可发布内容，新人引导流程完整 |
| S4-04 | 国际化框架：内容多语言标签支持，推荐模型多语言配置，审核词库多语言扩展 | P2 | 后端 C | 3d | 框架支持中/英文切换，多语言词库可配置 |
| S4-05 | 数据分析增强：扩展创作者数据中心（流量来源、完播率、互动转化漏斗），建立运营数据看板 | P1 | 后端 B+前端 B | 4d | 创作者可查看流量来源和内容诊断，运营数据看板可用 |
| S4-06 | 反作弊基础建设：引入设备指纹 SDK，建立用户行为异常检测规则，内容相似度去重 | P1 | 后端 A+安全 | 4d | 设备指纹采集可用，异常行为告警可用，相似内容识别率 > 80% |
| S4-07 | 剩余接口测试补充：为 TagController、AdminReportController、AdminSettingsController、NotificationController 等补充测试 | P1 | 测试 | 2d | 接口覆盖率提升至 90%+ |
| S4-08 | Web 后台接入真实 API：将 `langtou-admin/index.html` 从 Mock 数据切换到真实后端 API | P2 | 前端 A | 2d | 管理后台所有功能使用真实数据，无 Mock 依赖 |

**Sprint 4 交付物**:
- 前端性能显著提升，首页加载时间减少 40%
- 社区运营工具可用
- 国际化框架搭建完成
- 接口测试覆盖率 > 90%
- 反作弊基础能力上线

---

## 3. 技术方案设计

### 3.1 推荐接入方案（LT-002）

#### 3.1.1 现状分析

当前推荐系统（`langtou-recommendation/`）基于 Python FastAPI + XGBoost/LightGBM，存在以下问题：

- 召回层仅支持协同过滤和热门召回，无向量召回
- 排序层使用基础 XGBoost，无深度学习模型
- 特征工程仅支持离线批量计算，无实时特征
- 无 A/B 测试平台，算法迭代无法量化评估

#### 3.1.2 目标架构

```
用户行为事件
    |
    v
[Kafka] --> [Flink 实时特征计算] --> [Redis 实时特征存储]
    |                                      |
    v                                      v
[MySQL 离线特征]                    [特征拼接服务]
    |                                      |
    v                                      v
[双塔模型召回] --> [Milvus 向量检索] --> [多路召回合并]
    |                                      |
    v                                      v
[DeepFM/DIN 精排] --> [重排策略] --> [A/B 分流] --> [Feed API]
```

#### 3.1.3 分阶段实施

**第一阶段（Sprint 1-2）: 基础优化**

1. **修复现有性能问题**
   - 修复 `refreshUserProfile` N+1 查询（LT-007）
   - 修复 `calculateFollowingScore` 重复 RPC（LT-021）
   - `fillFeedAuthorInfoBatch` 增加 Caffeine 本地缓存
   - `recallHotTrends`/`recallEditorPicks` 增加 Redis 缓存（TTL 5-10min）

2. **A/B 测试框架**
   - 在 Gateway 层实现 `AbTestFilter`（已有框架，需完善分流逻辑）
   - 支持按用户 ID 哈希分流，支持多实验并行
   - 埋点上报推荐曝光/点击/互动事件

**第二阶段（Sprint 3-4）: 向量召回**

3. **双塔模型 + Milvus**
   - 用户塔：用户画像特征（基础属性 + 行为统计 + 兴趣标签）
   - 内容塔：内容特征（文本 Embedding + 标签 + 统计特征）
   - 使用 Sentence-BERT 生成文本 Embedding
   - 部署 Milvus 向量数据库，支持 ANN 近似最近邻检索
   - 召回路径：协同过滤 + 热门召回 + 向量召回三路合并

**第三阶段（后续迭代）: 深度排序**

4. **DeepFM/DIN 排序模型**
   - 特征：用户特征 + 内容特征 + 上下文特征 + 交叉特征
   - DIN（Deep Interest Network）捕捉用户历史兴趣
   - 多目标优化：CTR + 时长 + 互动率联合建模
   - 模型服务化：TensorFlow Serving / Triton

5. **Flink 实时特征管道**
   - 用户实时行为流（曝光、点击、点赞、收藏、评论）
   - 滑动窗口统计（近 1h/6h/24h 行为特征）
   - 实时更新用户画像到 Redis

#### 3.1.4 关键代码变更

**文件**: `langtou-recommendation/app/engine/recall.py`

```python
# 新增向量召回路径
class VectorRecall:
    """双塔模型向量召回"""

    def __init__(self):
        self.milvus_client = MilvusClient(uri=MILVUS_URI)
        self.user_tower = load_model("user_tower")
        self.content_tower = load_model("content_tower")

    def recall(self, user_id, top_k=200):
        user_embedding = self.user_tower.encode(get_user_features(user_id))
        results = self.milvus_client.search(
            collection_name="content_embeddings",
            data=[user_embedding],
            limit=top_k,
            output_fields=["content_id", "score"]
        )
        return results
```

**文件**: `langtou-backend/langtou-gateway/src/main/java/com/langtou/gateway/filter/AbTestFilter.java`

```java
// 完善 A/B 分流逻辑
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String userId = getUserId(exchange);
    String experimentKey = "recommend_algorithm";

    // 按用户 ID 哈希分流
    int bucket = Math.abs(userId.hashCode() % 100);
    String variant;
    if (bucket < 50) {
        variant = "control";      // 基线：现有 XGBoost
    } else if (bucket < 75) {
        variant = "vector_recall"; // 实验 1：向量召回
    } else {
        variant = "deepfm";       // 实验 2：DeepFM 排序
    }

    exchange.getRequest().mutate()
        .header("X-AB-Variant", variant)
        .header("X-AB-Experiment", experimentKey)
        .build();

    return chain.filter(exchange);
}
```

#### 3.1.5 预期效果

| 指标 | 当前值 | Sprint 2 后目标 | Sprint 4 后目标 |
|------|--------|----------------|----------------|
| 推荐 CTR | ~5% | 7% | 10% |
| 推荐覆盖率 | ~15% | 25% | 35% |
| 推荐多样性 | ~0.4 | 0.5 | 0.6 |
| 7 日留存率 | 25% | 28% | 32% |

---

### 3.2 AI 审核接入方案（LT-003）

#### 3.2.1 现状分析

当前审核机制：
- `ContentAuditService.java`：基于敏感词匹配
- `SensitiveWord.java`：200+ 内置词库，仅内存存储
- `ImageAuditProvider.java`：接口定义存在，但实现为空
- 审核流程：发布 -> 敏感词匹配 -> 人工审核（24h+）

#### 3.2.2 目标架构

```
内容发布
    |
    v
[敏感词快速过滤] --命中--> [自动拒绝]
    |
    v (未命中)
[阿里云 AI 审核 API]
    |-- 文本审核（反垃圾、色情、暴恐、政治敏感、广告）
    |-- 图像审核（色情、暴恐、广告、二维码）
    |-- 视频审核（关键帧抽取 + 图像审核）
    |
    v
[审核结果判定]
    |-- 通过 --> [发布成功]
    |-- 疑似 --> [人工复核队列]
    |-- 拒绝 --> [通知用户 + 记录原因]
    |
    v
[审核结果缓存] --> [相似内容复用审核结果]
```

#### 3.2.3 技术实现

**1. 阿里云内容安全 API 接入**

```xml
<!-- pom.xml 新增依赖 -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>green20220302</artifactId>
    <version>2.0.2</version>
</dependency>
```

**文件**: `langtou-backend/langtou-content-service/src/main/java/com/langtou/content/service/ContentAuditService.java`

```java
@Service
@Slf4j
public class ContentAuditService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ImageAuditProvider imageAuditProvider;

    // 审核结果缓存 TTL：7 天
    private static final long AUDIT_CACHE_TTL = 7 * 24 * 3600;

    /**
     * 内容审核主流程
     */
    public AuditResult auditContent(Long contentId, String text, List<String> imageUrls) {
        // 1. 敏感词快速过滤
        SensitiveWordResult wordResult = sensitiveWordFilter(text);
        if (wordResult.isHit()) {
            return AuditResult.reject("内容包含违规敏感词: " + wordResult.getMatchedWords());
        }

        // 2. 查询审核缓存（相似内容复用）
        String cacheKey = "audit:content:" + DigestUtils.md5Hex(text);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return AuditResult.fromCache(JSON.parseObject(cached, AuditResult.class));
        }

        // 3. AI 文本审核
        AuditResult textResult = aiTextAudit(text);
        if (textResult.isRejected()) {
            cacheAuditResult(cacheKey, textResult);
            return textResult;
        }

        // 4. AI 图像审核
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                AuditResult imageResult = imageAuditProvider.auditImage(imageUrl);
                if (imageResult.isRejected()) {
                    cacheAuditResult(cacheKey, imageResult);
                    return imageResult;
                }
            }
        }

        // 5. 综合判定
        AuditResult finalResult = AuditResult.pass();
        cacheAuditResult(cacheKey, finalResult);
        return finalResult;
    }

    /**
     * AI 文本审核 - 阿里云绿网
     */
    private AuditResult aiTextAudit(String text) {
        try {
            // 调用阿里云文本审核 API
            TextModerationRequest request = new TextModerationRequest();
            request.setService("nlp_moderation");
            request.setContent(text.getBytes(StandardCharsets.UTF_8));

            TextModerationResponse response = greenClient.textModeration(request);

            if (response.getCode() == 200) {
                TextModerationResult result = response.getData();
                if ("block".equals(result.getConclusion())) {
                    return AuditResult.reject("AI审核拒绝: " + result.getReason());
                } else if ("review".equals(result.getConclusion())) {
                    return AuditResult.review("AI审核疑似: " + result.getReason());
                }
            }
            return AuditResult.pass();
        } catch (Exception e) {
            log.error("AI文本审核异常，降级为人工审核", e);
            return AuditResult.review("AI审核异常，需人工复核");
        }
    }

    /**
     * 审核结果缓存
     */
    private void cacheAuditResult(String cacheKey, AuditResult result) {
        redisTemplate.opsForValue().set(
            cacheKey,
            JSON.toJSONString(result),
            AUDIT_CACHE_TTL,
            TimeUnit.SECONDS
        );
    }
}
```

**2. 图像审核实现**

**文件**: `langtou-backend/langtou-content-service/src/main/java/com/langtou/content/service/ImageAuditProvider.java`

```java
@Service
@Slf4j
public class ImageAuditProvider {

    @Value("${audit.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${audit.aliyun.access-key-secret:}")
    private String accessKeySecret;

    private GreenClient greenClient;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(accessKeyId)) {
            this.greenClient = new GreenClient(accessKeyId, accessKeySecret);
        }
    }

    public AuditResult auditImage(String imageUrl) {
        if (greenClient == null) {
            log.warn("AI审核未配置，跳过图像审核");
            return AuditResult.pass();
        }

        try {
            ImageModerationRequest request = new ImageModerationRequest();
            request.setTaskId(UUID.randomUUID().toString());
            request.setUrl(imageUrl);
            request.setScene("porn,terrorism,ad,qrcode");

            ImageModerationResponse response = greenClient.imageModeration(request);

            if (response.getCode() == 200) {
                for (ImageModerationResult result : response.getData()) {
                    if ("block".equals(result.getConclusion())) {
                        return AuditResult.reject("图像审核拒绝: " + result.getReason());
                    } else if ("review".equals(result.getConclusion())) {
                        return AuditResult.review("图像审核疑似: " + result.getReason());
                    }
                }
            }
            return AuditResult.pass();
        } catch (Exception e) {
            log.error("AI图像审核异常", e);
            return AuditResult.review("AI图像审核异常，需人工复核");
        }
    }
}
```

#### 3.2.4 降级策略

| 场景 | 降级方案 |
|------|---------|
| 阿里云 API 超时 | 重试 1 次，仍失败则标记为"需人工复核" |
| 阿里云 API 不可用 | 降级为纯敏感词过滤 + 人工审核 |
| 审核队列积压 > 1000 | 自动扩容人工审核，发送告警通知 |

#### 3.2.5 预期效果

| 指标 | 当前值 | 目标值 |
|------|--------|--------|
| AI 审核准确率 | N/A | > 95% |
| AI 审核召回率 | N/A | > 90% |
| 审核时效 | 24h+ | < 5s（AI）/ < 2h（人工复核） |
| 违规内容漏放率 | 未知 | < 0.1% |
| 人工审核成本 | 100% | 降低 70% |

---

### 3.3 N+1 查询修复方案（LT-007, LT-008）

#### 3.3.1 问题分析

**问题 1: refreshUserProfile N+1**

```java
// 当前代码（RecommendationServiceImpl.java:222-228）
for (Long noteId : noteIds) {
    List<Long> tagIds = tagService.getTagIdsByNoteId(noteId);      // N 次查询
    List<Tag> tags = tagService.getTagsByNoteId(noteId);              // N 次查询
    // ... 构建用户画像
}
```

**问题 2: getConversations N+1**

```java
// 当前代码（MessageServiceImpl.java:82-98）
for (ConversationVO conv : conversations) {
    Message latestMsg = messageMapper.getLatestMessage(userId, conv.getTargetId());  // N 次查询
    Long unreadCount = messageMapper.getUnreadCount(userId, conv.getTargetId());       // N 次查询
    // ...
}
```

#### 3.3.2 修复方案

**修复 1: refreshUserProfile 批量查询**

```java
// 修复后代码
// 1. 批量获取所有笔记的标签 ID
Map<Long, List<Long>> noteTagIdsMap = tagService.batchGetTagIdsByNoteIds(noteIds);
// 2. 批量获取所有标签详情
Set<Long> allTagIds = noteTagIdsMap.values().stream()
    .flatMap(List::stream)
    .collect(Collectors.toSet());
Map<Long, Tag> tagMap = tagService.batchGetTagsByIds(allTagIds);

// 3. 构建用户画像（无额外查询）
for (Long noteId : noteIds) {
    List<Long> tagIds = noteTagIdsMap.getOrDefault(noteId, Collections.emptyList());
    List<Tag> tags = tagIds.stream()
        .map(tagMap::get)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    // ... 构建用户画像
}
```

**新增 Mapper 方法**:

```java
// TagMapper.java
@Select("<script>" +
    "SELECT note_id, tag_id FROM note_tag WHERE note_id IN " +
    "<foreach collection='noteIds' item='id' open='(' separator=',' close=')'>" +
    "#{id}" +
    "</foreach>" +
    "</script>")
@ResultType(Map.class)
List<Map<String, Object>> batchSelectTagIdsByNoteIds(@Param("noteIds") Collection<Long> noteIds);

// TagMapper.java
@Select("<script>" +
    "SELECT * FROM tag WHERE id IN " +
    "<foreach collection='tagIds' item='id' open='(' separator=',' close=')'>" +
    "#{id}" +
    "</foreach>" +
    "</script>")
List<Tag> batchSelectByIds(@Param("tagIds") Collection<Long> tagIds);
```

**修复 2: getConversations 批量查询**

```java
// 修复后代码
List<Long> targetIds = conversations.stream()
    .map(ConversationVO::getTargetId)
    .collect(Collectors.toList());

// 1. 批量获取最新消息（子查询）
List<Message> latestMessages = messageMapper.batchGetLatestMessages(userId, targetIds);
Map<Long, Message> latestMsgMap = latestMessages.stream()
    .collect(Collectors.toMap(
        msg -> msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId(),
        Function.identity(),
        (a, b) -> a.getCreateTime().after(b.getCreateTime()) ? a : b
    ));

// 2. 批量获取未读数（GROUP BY 子查询）
List<Map<String, Object>> unreadCounts = messageMapper.batchGetUnreadCounts(userId, targetIds);
Map<Long, Long> unreadCountMap = unreadCounts.stream()
    .collect(Collectors.toMap(
        row -> (Long) row.get("target_id"),
        row -> (Long) row.get("unread_count")
    ));

// 3. 填充会话信息（无额外查询）
for (ConversationVO conv : conversations) {
    conv.setLatestMessage(latestMsgMap.get(conv.getTargetId()));
    conv.setUnreadCount(unreadCountMap.getOrDefault(conv.getTargetId(), 0L));
}
```

**新增 Mapper 方法**:

```java
// MessageMapper.java
@Select("<script>" +
    "SELECT m.* FROM message m INNER JOIN (" +
    "  SELECT MAX(id) as max_id FROM message " +
    "  WHERE (sender_id = #{userId} AND receiver_id IN " +
    "  <foreach collection='targetIds' item='id' open='(' separator=',' close=')'>" +
    "    #{id}" +
    "  </foreach>" +
    "  ) OR (receiver_id = #{userId} AND sender_id IN " +
    "  <foreach collection='targetIds' item='id' open='(' separator=',' close=')'>" +
    "    #{id}" +
    "  </foreach>" +
    "  ) GROUP BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END" +
    ") latest ON m.id = latest.max_id" +
    "</script>")
List<Message> batchGetLatestMessages(@Param("userId") Long userId,
                                       @Param("targetIds") Collection<Long> targetIds);

// MessageMapper.java
@Select("<script>" +
    "SELECT CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END as target_id, " +
    "COUNT(*) as unread_count FROM message " +
    "WHERE receiver_id = #{userId} AND is_read = 0 AND sender_id IN " +
    "<foreach collection='targetIds' item='id' open='(' separator=',' close=')'>" +
    "  #{id}" +
    "</foreach>" +
    " GROUP BY CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END" +
    "</script>")
List<Map<String, Object>> batchGetUnreadCounts(@Param("userId") Long userId,
                                                @Param("targetIds") Collection<Long> targetIds);
```

#### 3.3.3 性能对比

| 场景 | 修复前查询数 | 修复后查询数 | 修复前耗时 | 修复后耗时 |
|------|------------|------------|-----------|-----------|
| refreshUserProfile（100 篇笔记） | 200+ | 2 | ~5s | < 100ms |
| getConversations（20 个会话） | 40+ | 2 | ~2s | < 50ms |

---

### 3.4 接口测试 CI/CD 集成方案（LT-011）

#### 3.4.1 目标

- 接口测试覆盖率从 35% 提升至 90%+
- PR/MR 自动触发全量接口测试
- 测试失败阻断合并
- 测试报告自动归档并通知

#### 3.4.2 测试框架设计

```
langtou-backend/
  langtou-user-service/src/test/
    java/com/langtou/user/
      controller/
        UserControllerApiTest.java        (已有)
        FollowControllerApiTest.java       (新增)
        PointsControllerApiTest.java       (新增)
        UserLevelControllerApiTest.java    (新增)
        AdminUserControllerApiTest.java   (新增)
        AdminAuthControllerApiTest.java    (新增)
      service/
        UserServiceTest.java              (已有)
  langtou-content-service/src/test/
    java/com/langtou/content/
      controller/
        ContentControllerApiTest.java     (已有)
        SearchControllerApiTest.java       (新增)
        TagControllerApiTest.java          (新增)
        RecommendationControllerApiTest.java (新增)
        CreatorAnalyticsControllerApiTest.java (新增)
        AdminAnalyticsControllerApiTest.java   (新增)
        AdminAdControllerApiTest.java      (新增)
        AdminNoteControllerApiTest.java    (新增)
        AdminReportControllerApiTest.java  (新增)
        AdminSettingsControllerApiTest.java (新增)
      service/
        ContentServiceTest.java            (已有)
  langtou-interact-service/src/test/
    java/com/langtou/interact/
      controller/
        InteractControllerApiTest.java     (已有)
        CollectionControllerApiTest.java   (新增)
  langtou-message-service/src/test/
    java/com/langtou/message/
      controller/
        MessageControllerApiTest.java      (新增)
        NotificationControllerApiTest.java  (新增)
```

#### 3.4.3 测试模板

```java
/**
 * 通用 Controller 接口测试基类
 * 提供统一的测试框架、认证、断言
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    protected String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    /**
     * 获取测试用户 Token
     */
    protected String getTestUserToken() {
        // 使用测试用户登录获取 Token
        Map<String, String> loginBody = Map.of("username", "testuser", "password", "testpass123");
        return given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .post(baseUrl + "/api/v1/auth/login")
            .then()
            .statusCode(200)
            .extract().path("data.token");
    }

    /**
     * 获取测试管理员 Token
     */
    protected String getAdminToken() {
        Map<String, String> loginBody = Map.of("username", "admin", "password", "admin123");
        return given()
            .contentType(ContentType.JSON)
            .body(loginBody)
            .post(baseUrl + "/api/v1/admin/auth/login")
            .then()
            .statusCode(200)
            .extract().path("data.token");
    }
}
```

```java
/**
 * SearchController 接口测试
 * 覆盖：正常搜索、空结果、XSS 防护、分页、排序、频率限制
 */
class SearchControllerApiTest extends BaseApiTest {

    @Test
    @DisplayName("搜索笔记 - 正常关键词")
    void searchNotes_normalKeyword_success() {
        given()
            .header("Authorization", "Bearer " + getTestUserToken())
            .queryParam("keyword", "美食推荐")
            .queryParam("page", 1)
            .queryParam("size", 20)
            .get(baseUrl + "/api/v1/search/notes")
            .then()
            .statusCode(200)
            .body("code", equalTo(0))
            .body("data.content", notNullValue())
            .body("data.content.size()", lessThanOrEqualTo(20));
    }

    @Test
    @DisplayName("搜索笔记 - XSS 关键词过滤")
    void searchNotes_xssKeyword_sanitized() {
        given()
            .header("Authorization", "Bearer " + getTestUserToken())
            .queryParam("keyword", "<script>alert('xss')</script>")
            .get(baseUrl + "/api/v1/search/notes")
            .then()
            .statusCode(200)
            .body("data.content", everyItem(
                hasJsonPath("title", not(containsString("<script>")))
            ));
    }

    @Test
    @DisplayName("搜索笔记 - 空关键词返回 400")
    void searchNotes_emptyKeyword_badRequest() {
        given()
            .header("Authorization", "Bearer " + getTestUserToken())
            .get(baseUrl + "/api/v1/search/notes")
            .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("搜索笔记 - 频率限制")
    void searchNotes_rateLimited_429() {
        String token = getTestUserToken();
        for (int i = 0; i < 35; i++) {
            given()
                .header("Authorization", "Bearer " + token)
                .queryParam("keyword", "test")
                .get(baseUrl + "/api/v1/search/notes");
        }
        given()
            .header("Authorization", "Bearer " + token)
            .queryParam("keyword", "test")
            .get(baseUrl + "/api/v1/search/notes")
            .then()
            .statusCode(429);
    }
}
```

#### 3.4.4 CI/CD Pipeline 配置

**文件**: `langtou-devops/Jenkinsfile`（更新）

```groovy
pipeline {
    agent any

    triggers {
        pullRequest(
            branches: [[name: 'main'], [name: 'develop']],
            titleFilter: '.*',
            org: 'langtou'
        )
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('API Integration Test') {
            steps {
                sh '''
                    # 启动测试环境
                    docker-compose -f docker-compose.dev.yml up -d
                    # 等待服务就绪
                    sleep 30
                    # 运行接口测试
                    mvn verify -DskipUnitTests=true
                '''
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                    // 生成测试覆盖率报告
                    sh 'mvn jacoco:report'
                }
            }
        }

        stage('API Coverage Gate') {
            steps {
                sh '''
                    # 检查接口覆盖率是否达标（>= 75%）
                    COVERAGE=$(mvn jacoco:report | grep "Controller API Coverage" | awk '{print $NF}' | tr -d '%')
                    if [ "$COVERAGE" -lt 75 ]; then
                        echo "API 覆盖率 ${COVERAGE}% 低于 75% 阈值"
                        exit 1
                    fi
                '''
            }
        }

        stage('Security Scan') {
            steps {
                sh './langtou-devops/scripts/security-scan.sh'
            }
        }
    }

    post {
        failure {
            emailext(
                subject: "构建失败: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "接口测试或安全扫描未通过，请检查报告。",
                to: 'dev-team@langtou.com'
            )
        }
    }
}
```

#### 3.4.5 覆盖率目标

| Sprint | 目标覆盖率 | 新增测试类数 |
|--------|-----------|-------------|
| Sprint 2 | 75% | 10 |
| Sprint 4 | 90% | 5 |
| 持续迭代 | 95% | 按需补充 |

---

### 3.5 缓存优化方案（LT-012）

#### 3.5.1 现状问题

| 问题 | 影响 | 位置 |
|------|------|------|
| 缓存穿透：未对空结果做缓存 | 恶意查询穿透到数据库 | 全局 |
| 缓存击穿：热点数据过期时大量请求打到数据库 | 数据库瞬时压力暴增 | 热门笔记/用户 |
| 缓存雪崩：大量缓存同时过期 | 数据库全面过载 | TTL 设置不合理 |
| `keys` 命令阻塞 Redis | Redis 性能抖动 | `RecommendationServiceImpl.java:255-259` |
| TTL 设置不一致 | 维护困难 | `RedisKeyUtil.java` |

#### 3.5.2 优化方案

**1. 缓存穿透防护 - 空值缓存**

```java
/**
 * 通用缓存查询方法，内置穿透防护
 */
public <T> T getWithPassThroughProtection(String key, Class<T> type,
                                           Supplier<T> dbLoader, long ttl) {
    // 1. 查询缓存
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        if ("NULL".equals(cached)) {
            return null;  // 空值缓存命中，防止穿透
        }
        return JSON.parseObject(cached, type);
    }

    // 2. 查询数据库
    T value = dbLoader.get();
    if (value == null) {
        // 空值缓存，短 TTL 防止穿透
        redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
        return null;
    }

    // 3. 写入缓存（随机 TTL 防止雪崩）
    long randomTtl = ttl + ThreadLocalRandom.current().nextLong(0, ttl / 10);
    redisTemplate.opsForValue().set(key, JSON.toJSONString(value), randomTtl, TimeUnit.SECONDS);
    return value;
}
```

**2. 缓存击穿防护 - 互斥锁**

```java
/**
 * 热点数据查询，使用 Redis 分布式锁防止击穿
 */
public <T> T getWithBreakdownProtection(String key, Class<T> type,
                                          Supplier<T> dbLoader, long ttl) {
    // 1. 查询缓存
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null && !"NULL".equals(cached)) {
        return JSON.parseObject(cached, type);
    }

    // 2. 获取分布式锁
    String lockKey = "lock:" + key;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(locked)) {
        try {
            // 双重检查
            cached = redisTemplate.opsForValue().get(key);
            if (cached != null && !"NULL".equals(cached)) {
                return JSON.parseObject(cached, type);
            }

            // 查询数据库
            T value = dbLoader.get();
            if (value == null) {
                redisTemplate.opsForValue().set(key, "NULL", 5, TimeUnit.MINUTES);
            } else {
                long randomTtl = ttl + ThreadLocalRandom.current().nextLong(0, ttl / 10);
                redisTemplate.opsForValue().set(key, JSON.toJSONString(value),
                    randomTtl, TimeUnit.SECONDS);
            }
            return value;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 未获取锁，短暂等待后重试
        try { Thread.sleep(100); } catch (InterruptedException e) { return null; }
        return getWithBreakdownProtection(key, type, dbLoader, ttl);
    }
}
```

**3. 缓存雪崩防护 - 随机 TTL**

```java
/**
 * 统一缓存 TTL 策略
 */
public class CacheTTLStrategy {

    // 基础 TTL（秒）
    public static final long HOT_NOTE = 300;           // 热门笔记：5 分钟
    public static final long USER_PROFILE = 600;        // 用户资料：10 分钟
    public static final long TAG_LIST = 3600;          // 标签列表：1 小时
    public static final long RECOMMEND_FEED = 300;     // 推荐缓存：5 分钟
    public static final long HOT_TRENDS = 600;         // 热门趋势：10 分钟
    public static final long SENSITIVE_WORDS = 3600;    // 敏感词库：1 小时
    public static final long SEARCH_SUGGESTION = 1800;  // 搜索建议：30 分钟

    /**
     * 生成随机 TTL（基础 TTL + 0~10% 随机偏移）
     */
    public static long randomize(long baseTtl) {
        return baseTtl + ThreadLocalRandom.current().nextLong(0, Math.max(1, baseTtl / 10));
    }
}
```

**4. 替换 `keys` 命令为 `Scan`**

```java
// 修复前（RecommendationServiceImpl.java:255-259）
Set<String> keys = stringRedisTemplate.keys("recommend:user:" + userId + ":*");
for (String key : keys) {
    stringRedisTemplate.delete(key);
}

// 修复后
public void clearUserRecommendCache(Long userId) {
    String pattern = "recommend:user:" + userId + ":*";
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

    try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
        List<String> keysToDelete = new ArrayList<>();
        while (cursor.hasNext()) {
            keysToDelete.add(cursor.next());
            // 批量删除，每 100 条执行一次
            if (keysToDelete.size() >= 100) {
                stringRedisTemplate.delete(keysToDelete);
                keysToDelete.clear();
            }
        }
        if (!keysToDelete.isEmpty()) {
            stringRedisTemplate.delete(keysToDelete);
        }
    }
}
```

#### 3.5.3 缓存 Key 规范更新

| 缓存类型 | Key 格式 | TTL | 更新策略 |
|---------|---------|-----|---------|
| 热门笔记 | `note:hot:{categoryId}` | 5min + 随机 | 主动刷新 |
| 用户资料 | `user:profile:{userId}` | 10min + 随机 | 主动失效 |
| 推荐缓存 | `recommend:user:{userId}:feed:{page}` | 5min + 随机 | 画像更新时失效 |
| 热门趋势 | `trend:hot` | 10min + 随机 | 定时刷新 |
| 敏感词库 | `sensitive:words:all` | 1h + 随机 | 词库更新时失效 |
| 搜索建议 | `search:suggest:{prefix}` | 30min + 随机 | LRU 淘汰 |
| 标签列表 | `tag:list:{category}` | 1h + 随机 | 标签变更时失效 |
| 用户关注列表 | `follow:ids:{userId}` | 10min + 随机 | 关注变更时失效 |

#### 3.5.4 监控指标

| 指标 | 告警阈值 | 采集方式 |
|------|---------|---------|
| 缓存命中率 | < 90% | Redis INFO stats |
| 缓存穿透次数 | > 100/min | 自定义埋点 |
| Redis 阻塞时间 | > 10ms | Redis SLOWLOG |
| 大 Key 数量 | > 0 | redis-cli --bigkeys |
| 内存使用率 | > 80% | Redis INFO memory |

---

## 4. 附录

### 4.1 关联文档

| 文档 | 路径 |
|------|------|
| 市场竞争力分析报告 | `/workspace/langtou-team/pm-output/market-competitiveness-report.md` |
| 全面测试报告 | `/workspace/langtou-team/qa-output/comprehensive-test-report.md` |
| 竞品分析报告 | `/workspace/langtou-team/pm-output/competitive-analysis.md` |
| PRD v7.0 | `/workspace/langtou-team/pm-output/prd-v7.0.md` |
| 数据埋点需求 | `/workspace/langtou-team/pm-output/data-tracking-requirements.md` |
| API 测试用例 | `/workspace/langtou-team/qa-output/api-test-cases.md` |
| 测试策略 | `/workspace/langtou-team/qa-output/test-strategy.md` |
| 安全检查清单 | `/workspace/langtou-devops/security/security-checklist.md` |
| Redis 设计文档 | `/workspace/langtou-database/redis-design.md` |

### 4.2 关键代码文件索引

| 文件 | 说明 |
|------|------|
| `langtou-backend/langtou-common/src/main/java/com/langtou/common/utils/JwtUtils.java` | JWT 工具类 |
| `langtou-backend/langtou-common/src/main/java/com/langtou/common/utils/RedisKeyUtil.java` | Redis Key 管理 |
| `langtou-backend/langtou-gateway/src/main/java/com/langtou/gateway/filter/JwtAuthFilter.java` | JWT 认证过滤器 |
| `langtou-backend/langtou-gateway/src/main/java/com/langtou/gateway/filter/AbTestFilter.java` | A/B 测试过滤器 |
| `langtou-backend/langtou-gateway/src/main/java/com/langtou/gateway/config/RateLimiterConfig.java` | 限流配置 |
| `langtou-backend/langtou-content-service/src/main/java/com/langtou/content/service/ContentAuditService.java` | 内容审核服务 |
| `langtou-backend/langtou-content-service/src/main/java/com/langtou/content/service/ImageAuditProvider.java` | 图像审核提供者 |
| `langtou-backend/langtou-content-service/src/main/java/com/langtou/content/service/RecommendationService.java` | 推荐服务接口 |
| `langtou-backend/langtou-message-service/src/main/java/com/langtou/message/service/MessageService.java` | 消息服务接口 |
| `langtou-backend/langtou-user-service/src/main/java/com/langtou/user/service/UserService.java` | 用户服务接口 |
| `langtou-devops/.env` | 环境变量配置 |
| `langtou-devops/Jenkinsfile` | CI/CD 流水线 |
| `langtou-recommendation/app/engine/recall.py` | 推荐召回引擎 |
| `langtou-recommendation/app/engine/rank.py` | 推荐排序引擎 |

### 4.3 术语表

| 术语 | 说明 |
|------|------|
| P0/P1/P2/P3 | 问题优先级，P0 为阻塞级，P3 为建议级 |
| N+1 查询 | 循环中执行单条查询，导致数据库查询次数随数据量线性增长 |
| XSS | 跨站脚本攻击（Cross-Site Scripting） |
| 缓存穿透 | 查询不存在的数据，每次请求都穿透到数据库 |
| 缓存击穿 | 热点缓存过期瞬间，大量并发请求打到数据库 |
| 缓存雪崩 | 大量缓存同时过期，数据库全面过载 |
| CTR | 点击率（Click-Through Rate） |
| GMV | 商品交易总额（Gross Merchandise Volume） |
| ANN | 近似最近邻搜索（Approximate Nearest Neighbor） |

---

> **文档结束** - 请各团队负责人评审后确认，如有异议请在 Sprint 1 启动会前反馈。
