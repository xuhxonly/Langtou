# 🔨 Langtou (榔头) - 开源内容社区框架

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
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

## 🤝 如何贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

请确保遵循我们的[开发规范](docs/guides/development-guidelines.md)。

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
