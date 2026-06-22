# 🔨 Langtou (榔头) - 开源内容社区框架

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![CI: Backend](https://github.com/xuhxonly/Langtou/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/xuhxonly/Langtou/actions/workflows/backend-ci.yml)
[![CI: Frontend](https://github.com/xuhxonly/Langtou/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/xuhxonly/Langtou/actions/workflows/frontend-ci.yml)
[![CI: Python](https://github.com/xuhxonly/Langtou/actions/workflows/python-ci.yml/badge.svg)](https://github.com/xuhxonly/Langtou/actions/workflows/python-ci.yml)
[![CD: Deploy](https://github.com/xuhxonly/Langtou/actions/workflows/cd.yml/badge.svg)](https://github.com/xuhxonly/Langtou/actions/workflows/cd.yml)
[![Stars](https://img.shields.io/badge/stars-welcome-brightgreen.svg)]()
[![Issues](https://img.shields.io/badge/issues-welcome-red.svg)]()

> 一个类似小红书的社交内容社区全栈开源框架，基于微服务架构，支持图文/视频内容分享、个性化推荐、即时通讯等核心功能。

## ✨ 功能特性

- 📝 **内容创作**：支持图文、视频笔记发布
- 🔍 **智能推荐**：基于协同过滤的个性化 Feed 流
- 💬 **即时通讯**：WebSocket 私信、系统通知
- 👥 **社交关系**：关注、粉丝、互动
- 🏪 **商品橱窗**：创作者变现
- 📊 **数据分析**：内容表现统计
- 🛠️ **管理后台**：内容审核、用户管理
- 📱 **跨平台**：Web、iOS、Android

## 🏗️ 项目结构

```
langtou/
├── src/                           # 源代码
│   ├── backend/                   # 后端微服务 (Spring Boot)
│   ├── frontend/                  # 前端应用 (Vue 3)
│   │   ├── web/                  #   用户端 Web
│   │   └── admin/                #   管理后台
│   ├── mobile/                    # 移动端 (React Native)
│   └── recommendation/           # 推荐系统 (Python/FastAPI)
├── infrastructure/                # 基础设施
│   ├── database/                 #   数据库设计
│   └── devops/                  #   运维部署
├── docs/                          # 项目文档
└── scripts/                       # 工具脚本
```

## 🚀 快速开始

### 环境要求

- ☕ Java 21+
- 🍃 Maven 3.9+
- 🟢 Node.js 20+
- 🐳 Docker 24+
- 🗄️ MySQL 8+
- 🔴 Redis 7+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/langtou.git
cd langtou
```

### 2. 配置环境变量

```bash
cd infrastructure/devops
cp .env.example .env
# 编辑 .env 文件，填入你的实际配置
```

### 3. 启动基础设施

```bash
# 启动 MySQL 和 Redis
docker compose -f docker-compose.local.yml up -d

# 或者启动完整环境（包括 Kafka、ES、Nacos 等）
docker compose -f docker-compose.dev.yml --env-file .env up -d
```

### 4. 启动后端服务

```bash
cd src/backend
mvn clean install -DskipTests

# 分别启动各服务
mvn spring-boot:run -pl langtou-gateway
mvn spring-boot:run -pl langtou-user-service
mvn spring-boot:run -pl langtou-content-service
# ... 其他服务
```

### 5. 启动前端

```bash
# Web 用户端
cd src/frontend/web
npm install
npm run dev

# 管理后台
cd ../admin
npm install
npm run dev
```

### 6. 启动移动端

```bash
cd src/mobile
npm install
npx react-native run-android  # Android
npx react-native run-ios      # iOS
```

## 📚 文档

- [快速入门指南](docs/guides/getting-started.md)
- [架构设计](docs/architecture/)
- [API 文档](infrastructure/devops/docs/)
- [数据库设计](infrastructure/database/)
- [开发规范](docs/guides/development-guidelines.md)

## 🔄 CI/CD

本项目使用 GitHub Actions 实现自动化测试和部署：

### CI Workflows

| Workflow | 触发条件 | 功能 |
|----------|---------|------|
| [backend-ci.yml](.github/workflows/backend-ci.yml) | PR 到 main/develop | Java 单元测试、代码检查 |
| [frontend-ci.yml](.github/workflows/frontend-ci.yml) | PR 到 main/develop | Vue/React 测试、构建 |
| [python-ci.yml](.github/workflows/python-ci.yml) | PR 到 main/develop | Python lint、测试 |

### CD Workflow

| Workflow | 触发条件 | 功能 |
|----------|---------|------|
| [cd.yml](.github/workflows/cd.yml) | push 到 main | Docker 构建、推送到 GHCR、部署 |

### 需要的 Secrets

部署前请在 GitHub 仓库设置以下 Secrets：

```
STAGING_HOST       - 测试环境服务器地址
STAGING_USER       - 测试环境 SSH 用户名
STAGING_SSH_KEY    - 测试环境 SSH 私钥
```

## 🤝 如何贡献

欢迎贡献代码！我们非常期待你的参与。

- 📖 [贡献指南](CONTRIBUTING.md) - 详细了解贡献流程
- 🐛 [报告 Bug](https://github.com/xuhxonly/Langtou/issues/new?template=bug-report.yml) - 使用 Bug 模板
- ✨ [功能请求](https://github.com/xuhxonly/Langtou/issues/new?template=feature-request.yml) - 提交功能建议
- 🔒 [安全政策](SECURITY.md) - 了解如何报告安全漏洞

### 快速开始

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

请确保遵循我们的[开发规范](docs/guides/development-guidelines.md)和[代码提交规范](CONTRIBUTING.md#提交规范)。

## 💖 支持项目

如果这个项目对你有帮助，请给我们一个 ⭐️ Star！

你也可以通过以下方式支持我们：

- 📝 在 Issue 里反馈问题
- 🔀 提交 Pull Request
- 📣 在社交媒体分享

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

Made with ❤️ by the Langtou Team
