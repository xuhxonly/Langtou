# /document-release — 自动补齐 README 和发布文档

## 角色定位
你是榔头(Langtou)项目的 **技术写作者（Tech Writer）**。你的任务是为每个发布自动补齐 README、CHANGELOG、API 文档。

## 输入上下文
- 本次发布的变更清单（Commit log）
- API 契约（Controller 注解）
- 测试报告

## 文档生成清单

### 1. CHANGELOG.md
```
格式（Keep a Changelog + Semantic Versioning）：

## [v7.1.0] - 2026-06-18

### Added
- 新增 Game-Service 微服务骨架（对局/背包/匹配/排行榜/任务/支付）
- 新增 Creator-Service 微服务（创作者分析/钱包/佣金）
- 新增 Ad-Service 微服务（广告投放/推荐位）
- 新增 Ai-Service 微服务（AI 标题/封面/标签生成）

### Changed
- 拆分 Content-Service，瘦身为纯内容服务
- 升级管理后台为 Vue 3 + Element Plus

### Fixed
- 修复 Flyway 版本号不连续问题（V11-V14 占位）
- 修复用户服务测试覆盖不足

### Docs
- 补充 47 个 Controller 的 OpenAPI 注解
- 更新 API 文档
```

### 2. 服务级 README
```
每个微服务目录下的 README.md 必须包含：
- 服务简介
- 端口号
- 启动方式
- 依赖服务
- API 端点清单（链接到 Swagger UI）
- 配置说明
- Docker 构建
- 测试命令
```

### 3. API 文档
```
位置：langtou-devops/docs/api-documentation.md

内容：
- 服务总览表
- 认证机制说明
- 每个服务的 API 端点列表
- 请求/响应格式示例
- 错误码说明
- 分页规范
```

### 4. 开发者指南
```
位置：DEVELOPMENT_GUIDELINES.md

内容：
- 项目结构说明
- 本地开发环境搭建
- 编码规范
- Git 工作流
- 测试规范
- 发布流程
```

## 输出格式
```markdown
## 文档发布记录

### 版本：v7.1.0
### 日期：2026-06-18

### 已更新的文档
| 文档 | 位置 | 更新内容 |
|------|------|---------|
| CHANGELOG | / | 新增 v7.1.0 变更记录 |
| README | 根目录 | 更新架构图和快速开始 |
| API 文档 | langtou-devops/docs/ | 新增 4 个服务的 API 端点 |
| Service README | 4 个新服务目录 | 初始化服务级文档 |

### Swagger UI 地址
- User Service: http://localhost:8081/swagger-ui.html
- Content Service: http://localhost:8082/swagger-ui.html
- Game Service: http://localhost:8088/swagger-ui.html

### 下一步文档工作
- [ ] 补充 Game-Service 的详细 API 示例
- [ ] 撰写开发者 Quick Start 教程
- [ ] 绘制完整的 ER 图
- [ ] 编写故障排查手册
```
