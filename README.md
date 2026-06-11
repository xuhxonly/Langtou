# 榔头 (Langtou) - 社交内容社区APP

> 一个类似小红书的社交内容社区全栈应用，采用微服务架构，支持图文/视频内容分享、个性化推荐、即时通讯等核心功能。

---

## 项目架构概览

```
langtou/
├── langtou-backend/          # 后端微服务 (Spring Boot + Go)
├── langtou-mobile/           # 移动端应用 (React Native)
├── langtou-database/         # 数据库设计与迁移
├── langtou-devops/           # DevOps基础设施
├── langtou-recommendation/   # 推荐系统 (Python/FastAPI)
└── xiaohongshu-architecture/ # 技术架构文档
```

---

## 技术栈

| 层级 | 技术选型 |
|------|---------|
| **前端** | React Native 0.76+, TypeScript, Zustand, React Query |
| **后端** | Spring Boot 3.x, Spring Cloud Gateway, Go Gin |
| **数据库** | MySQL 8, Redis Cluster, Elasticsearch, MongoDB |
| **消息队列** | Apache Kafka |
| **推荐系统** | Python, FastAPI, XGBoost, LightGBM |
| **DevOps** | Docker, Kubernetes, Jenkins, Prometheus, Grafana |

---

## 核心服务

### 后端微服务

| 服务 | 端口 | 说明 |
|------|------|------|
| `langtou-gateway` | 8080 | API网关，统一鉴权、限流、路由 |
| `langtou-user-service` | 8081 | 用户服务，注册/登录/用户管理 |
| `langtou-content-service` | 8082 | 内容服务，笔记发布/管理 |
| `langtou-interact-service` | 8083 | 互动服务，点赞/评论/收藏 |
| `langtou-message-service` | 8084 | 消息服务，私信/通知 |
| `langtou-recommendation` | 8000 | 推荐服务，个性化Feed |

---

## 快速开始

### 1. 启动基础设施

```bash
cd langtou-devops
./scripts/start-all.sh
```

### 2. 初始化数据库

```bash
./scripts/init-db.sh
```

### 3. 启动后端服务

```bash
cd langtou-backend
mvn clean install
mvn spring-boot:run -pl langtou-gateway
mvn spring-boot:run -pl langtou-user-service
# ... 其他服务
```

### 4. 启动移动端

```bash
cd langtou-mobile/LangtouMobile
npm install
npx react-native run-ios    # iOS
npx react-native run-android # Android
```

### 5. 启动推荐服务

```bash
cd langtou-recommendation/recommendation-service
pip install -r requirements.txt
python main.py
```

---

## 项目模块说明

### langtou-backend
Spring Boot 微服务集群，包含：
- 统一响应封装、全局异常处理、JWT鉴权
- MyBatis-Plus 数据访问
- OpenFeign 服务间调用
- 每个服务独立 Dockerfile

### langtou-mobile
React Native 跨端应用，包含：
- 双列瀑布流首页
- 图文/视频发布
- 私信聊天
- 个人中心
- 深色/浅色主题

### langtou-database
- 完整的 MySQL 表结构设计 (10张核心表)
- Redis 缓存设计文档
- Elasticsearch 索引 Mapping
- Flyway 数据库迁移

### langtou-devops
- Docker Compose 编排 (开发/生产环境)
- Kubernetes 部署配置
- Jenkins CI/CD 流水线
- Prometheus + Grafana 监控
- Nginx 反向代理

### langtou-recommendation
- 多路召回 (协同过滤/内容相似/热度/画像)
- XGBoost/LightGBM 排序模型
- 多样性重排 (MMR算法)
- FastAPI 服务接口

---

## 开发团队

| 角色 | 职责 |
|------|------|
| 后端架构师 | 微服务框架搭建、服务拆分、API设计 |
| 前端工程师 | React Native 开发、UI组件、状态管理 |
| 数据库工程师 | 数据库设计、缓存策略、ES索引 |
| DevOps工程师 | CI/CD、Docker/K8s、监控告警 |
| 推荐算法工程师 | 召回/排序/重排算法、模型训练 |

---

## 文档

- [技术架构文档](xiaohongshu-architecture/xiaohongshu-architecture.html)
- [后端服务 README](langtou-backend/README.md)
- [移动端 README](langtou-mobile/LangtouMobile/README.md)
- [数据库设计 README](langtou-database/README.md)
- [DevOps README](langtou-devops/README.md)
- [推荐系统 README](langtou-recommendation/README.md)

---

## 许可证

MIT License
