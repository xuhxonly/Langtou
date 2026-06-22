# Langtou (榔头) DevOps 基础设施

榔头是一个类似小红书的社交内容社区APP的DevOps配置仓库,提供完整的Docker容器化、Kubernetes编排、Jenkins CI/CD和监控告警解决方案。

## 目录结构

```
langtou-devops/
├── docker-compose.yml       # 生产环境 Docker Compose 配置
├── docker-compose.dev.yml   # 开发环境轻量版配置
├── .env                     # 环境变量文件
├── Jenkinsfile              # Jenkins CI/CD Pipeline
├── README.md                # 本文档
│
├── nginx/                   # Nginx 配置
│   ├── nginx.conf           # 主配置文件
│   └── h5.conf              # H5前端配置
│
├── k8s/                     # Kubernetes 配置
│   ├── namespace.yml        # 命名空间
│   ├── configmap.yml        # ConfigMap 配置
│   ├── secret.yml           # Secret 配置
│   ├── services/            # 微服务 Deployment/Service
│   │   ├── gateway.yml
│   │   ├── user-service.yml
│   │   ├── content-service.yml
│   │   ├── interact-service.yml
│   │   └── message-service.yml
│   └── ingress/
│       └── ingress.yml      # Ingress 配置
│
├── monitoring/              # 监控配置
│   ├── prometheus.yml       # Prometheus 配置
│   ├── alertmanager.yml     # Alertmanager 配置
│   ├── alert-rules.yml      # 告警规则
│   ├── grafana-dashboard.json  # Grafana 仪表盘
│   └── grafana-provisioning/   # Grafana 自动配置
│       ├── dashboards/
│       └── datasources/
│
└── scripts/                 # 运维脚本
    ├── start-all.sh         # 一键启动
    ├── stop-all.sh          # 服务管理 (停止/清理/状态/日志/重启)
    ├── deploy-k8s.sh        # K8s 部署
    ├── init-db.sh           # 数据库初始化
    ├── mysql-master.cnf     # MySQL 主库配置
    ├── mysql-slave.cnf      # MySQL 从库配置
    └── redis-cluster.conf    # Redis Cluster 配置
```

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户访问层                                      │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                  │
│  │   React     │    │   React     │    │   H5        │                  │
│  │   Native    │    │   Native    │    │   Web       │                  │
│  │   iOS       │    │   Android   │    │             │                  │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                  │
└─────────┼─────────────────┼─────────────────┼───────────────────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
                    ┌───────▼───────┐
                    │    Nginx      │
                    │  (反向代理)     │
                    │  (负载均衡)     │
                    │  (静态资源)     │
                    └───────┬───────┘
                            │
┌───────────────────────────┼─────────────────────────────────────────────┐
│                     API 网关层                                           │
│                   ┌──────▼──────┐                                        │
│                   │   Gateway   │                                        │
│                   │   Service    │                                        │
│                   │  (Spring    │                                        │
│                   │   Cloud      │                                        │
│                   │   Gateway)   │                                        │
│                   └──────┬──────┘                                        │
│                          │                                               │
└──────────────────────────┼───────────────────────────────────────────────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────▼─────┐   ┌──────▼──────┐  ┌────▼─────────┐
    │   User    │   │   Content   │  │   Interact   │
    │  Service  │   │   Service   │  │   Service    │
    │  用户服务  │   │   内容服务   │  │   互动服务    │
    └─────┬─────┘   └──────┬──────┘  └────┬─────────┘
          │                │              │
          │                │              │
┌─────────┼────────────────┼──────────────┼───────────────────────────────┐
│         │          数据存储层            │                                │
│         │                              │                                │
│    ┌────▼────┐    ┌─────────────┐  ┌───▼───┐                          │
│    │  MySQL  │    │Elasticsearch│  │ Redis │                          │
│    │  8.0    │    │   8.x       │  │Cluster│                          │
│    │ (主从)  │    │  (搜索)     │  │ (缓存) │                          │
│    └─────────┘    └─────────────┘  └───────┘                          │
│                                                                    K8s │
│    ┌─────────────┐    ┌─────────────┐    ┌──────────────────┐        │
│    │   Kafka     │    │   Nacos     │    │Recommendation    │        │
│    │  (消息队列)  │    │ (注册中心)   │    │Service (Python)  │        │
│    └─────────────┘    └─────────────┘    └──────────────────┘        │
└────────────────────────────────────────────────────────────────────────┘
                           │
┌──────────────────────────┼────────────────────────────────────────────┐
│                     监控层                                               │
│   ┌──────────────┐  ┌────▼─────┐  ┌──────────────┐                     │
│   │ Prometheus   │  │ Grafana  │  │ Alertmanager │                     │
│   │ (指标采集)    │  │ (可视化)  │  │ (告警通知)   │                     │
│   └──────────────┘  └──────────┘  └──────────────┘                     │
└────────────────────────────────────────────────────────────────────────┘
```

## 环境要求

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境 | 4核 | 8GB | 50GB |
| 生产环境 | 8核+ | 16GB+ | 100GB+ |

### 软件要求

- Docker 24.x+
- Docker Compose 2.x+
- Bash 4.x+
- (可选) Kubernetes 1.28+ (生产部署)
- (可选) Jenkins 2.x (CI/CD)

## 快速开始

### 3步启动生产环境

```bash
cd /workspace/langtou-v3/langtou-devops

# 第1步: 确保脚本可执行
chmod +x ./scripts/*.sh

# 第2步: 一键启动所有服务 (自动检查环境、启动基础设施、初始化数据库、启动业务服务)
./scripts/start-all.sh

# 第3步: 验证服务状态
./scripts/stop-all.sh status
```

### 开发环境快速启动

```bash
cd /workspace/langtou-v3/langtou-devops

# 启动开发环境 (轻量版, 单实例MySQL/Redis, 无监控)
./scripts/start-all.sh dev

# 或手动使用 docker compose
docker compose -f docker-compose.dev.yml up -d
```

### 可选: 启用监控 (开发环境)

```bash
# 开发环境启用 Prometheus + Grafana
docker compose -f docker-compose.dev.yml --profile monitoring up -d
```

## 服务端口列表

| 服务 | 端口 | 描述 |
|------|------|------|
| Nginx | 80/443 | 反向代理/负载均衡 |
| Gateway | 8080 | API 网关 |
| User Service | 8081 | 用户服务 |
| Content Service | 8082 | 内容服务 |
| Interact Service | 8083 | 互动服务 |
| Message Service | 8084 | 消息服务 |
| Recommendation | 8000 | Python推荐服务 |
| Nacos | 8848 | 注册中心/配置中心 |
| MySQL Master | 3306 | 数据库主库 |
| MySQL Slave | 3307 | 数据库从库 |
| Redis Cluster | 6379-6384 | 缓存集群 (6节点) |
| Kafka | 9092 | 消息队列 |
| Elasticsearch | 9200 | 搜索引擎 |
| Kibana | 5601 | ES 可视化 |
| Prometheus | 9090 | 指标监控 |
| Grafana | 3000 | 可视化仪表盘 |

## 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| Nacos | nacos | nacos |
| MySQL | root | Langtou@Root2024 |
| MySQL | langtou | Langtou@Db2024 |
| Redis | - | Langtou@Redis2024 |
| Grafana | admin | Langtou@Grafana2024 |
| Elasticsearch | elastic | Langtou@Es2024 |

## 启动脚本详解

### start-all.sh

```bash
# 启动所有服务 (生产环境, 默认)
./scripts/start-all.sh

# 仅启动基础设施 (MySQL, Redis, Kafka, ES, Nacos)
./scripts/start-all.sh infra

# 仅启动监控服务 (Prometheus, Grafana)
./scripts/start-all.sh monitoring

# 仅启动业务服务 (Gateway, 微服务, Nginx)
./scripts/start-all.sh services

# 初始化数据库
./scripts/start-all.sh init-db

# 启动开发环境
./scripts/start-all.sh dev
```

### stop-all.sh

```bash
# 停止所有服务
./scripts/stop-all.sh stop

# 停止指定服务
./scripts/stop-all.sh stop gateway

# 停止并彻底清理数据卷 (警告: 数据将丢失!)
./scripts/stop-all.sh clean

# 查看服务状态和资源使用
./scripts/stop-all.sh status

# 持续跟踪查看日志
./scripts/stop-all.sh logs
./scripts/stop-all.sh logs gateway

# 查看最近N行日志
./scripts/stop-all.sh tail gateway 50

# 重启服务
./scripts/stop-all.sh restart
./scripts/stop-all.sh restart user-service
```

### init-db.sh

```bash
# 初始化数据库 (创建库、表、Nacos配置表)
./scripts/init-db.sh

# 初始化并插入测试数据
./scripts/init-db.sh --test-data

# 使用环境变量指定连接信息
DB_HOST=192.168.1.100 DB_ROOT_PASSWORD=mypassword ./scripts/init-db.sh
```

## 常见问题排查

### Q: Docker Compose 命令找不到?

```bash
# 检查 Docker Compose 版本
docker compose version
# 或
docker-compose --version

# 如果只有 docker-compose (v1), 脚本会自动适配
```

### Q: MySQL 启动失败或连接超时?

```bash
# 检查 MySQL 容器日志
docker compose logs mysql-master

# 手动检查 MySQL 状态
docker exec langtou-mysql-master mysqladmin ping -h localhost -u root -pLangtou@Root2024

# 如果数据卷损坏,清理后重启
./scripts/stop-all.sh clean
./scripts/start-all.sh
```

### Q: Redis Cluster 初始化失败?

```bash
# 手动初始化 Redis Cluster
docker exec -it langtou-redis-1 redis-cli --cluster create \
  172.20.0.20:6379 172.20.0.21:6380 172.20.0.22:6381 \
  172.20.0.23:6382 172.20.0.24:6383 172.20.0.25:6384 \
  --cluster-replicas 1 -a Langtou@Redis2024

# 检查 Redis 节点状态
docker exec -it langtou-redis-1 redis-cli -a Langtou@Redis2024 cluster info
```

### Q: Nacos 无法连接 MySQL?

```bash
# 确保 MySQL 完全启动后再启动 Nacos
./scripts/start-all.sh infra
# 等待 MySQL healthy 后
./scripts/start-all.sh services

# 查看 Nacos 日志
docker compose logs nacos
```

### Q: 微服务启动失败?

```bash
# 查看具体服务日志
./scripts/stop-all.sh logs user-service

# 检查服务依赖是否就绪
./scripts/stop-all.sh status

# 重启单个服务
./scripts/stop-all.sh restart user-service
```

### Q: Elasticsearch 启动失败 (内存不足)?

```bash
# 修改 .env 中的 JVM 参数
# ES_JAVA_OPTS=-Xms512m -Xmx512m

# 或临时修改 docker-compose.yml
# 减少 ES 内存分配
```

### Q: 端口被占用?

```bash
# 检查端口占用
sudo lsof -i :3306
sudo lsof -i :8080

# 修改 .env 中的端口配置
# MYSQL_PORT=3307
# GATEWAY_PORT=8081
```

## 日志查看方法

```bash
# 查看所有服务日志 (实时跟踪)
./scripts/stop-all.sh logs

# 查看指定服务日志
docker compose logs -f gateway
docker compose logs -f user-service

# 查看最近100行日志
./scripts/stop-all.sh tail gateway

# 查看最近500行日志
./scripts/stop-all.sh tail gateway 500

# 查看特定时间段的日志
docker compose logs --since 2024-01-01T10:00:00 gateway

# 导出日志到文件
docker compose logs gateway > gateway.log 2>&1
```

## CI/CD 流程

```
代码提交
    │
    ▼
┌─────────────────┐
│  Jenkins       │
│  Pipeline      │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│ 代码   │ │ Maven │
│ 拉取   │ │ 编译   │
└────────┘ └────────┘
    │         │
    │         ▼
    │ ┌────────────┐
    │ │ 单元测试   │
    │ └────────────┘
    │         │
    └────┬────┘
         ▼
┌─────────────────┐
│ Docker Build    │
│ 并行构建所有服务 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Push to Registry│
│ 推送镜像        │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│ 自动   │ │ 手动   │
│ 部署Dev│ │ 部署Prod│
└────────┘ └────────┘
         │
         ▼
┌─────────────────┐
│ Health Check    │
│ 健康检查        │
└─────────────────┘
```

### 分支策略

| 分支 | 触发行为 |
|------|----------|
| `feature/*` | 代码质量检查 + 构建 + 部署到Dev |
| `develop` | 完整CI/CD + 自动部署到Dev |
| `release/*` | 完整CI/CD + 安全扫描 + 手动部署到Staging |
| `main`/`master` | 完整CI/CD + 安全扫描 + 手动部署到Prod |

## 监控告警

### Prometheus 指标

- JVM 指标 (堆内存、GC、线程)
- HTTP 请求指标 (QPS、延迟、错误率)
- 数据库连接池指标
- 基础设施指标 (CPU、内存、磁盘)

### 告警规则

- 服务宕机: 立即告警
- 错误率 > 5%: 2分钟后告警
- 响应时间 P95 > 1s: 3分钟后告警
- JVM堆内存 > 80%: 5分钟后告警
- 节点 CPU > 80%: 5分钟后告警

### 告警通知

支持多渠道通知:
- Email
- Slack
- 企业微信
- 钉钉 Webhook

## 环境变量

详见 `.env` 文件,主要配置项:

```bash
# 镜像仓库
DOCKER_REGISTRY=docker.io.local
DOCKER_NAMESPACE=langtou

# 数据库
MYSQL_ROOT_PASSWORD=Langtou@Root2024
MYSQL_DATABASE=langtou

# Redis
REDIS_PASSWORD=Langtou@Redis2024

# Nacos
NACOS_AUTH_ENABLE=true

# JVM
JVM_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

## 维护

### 备份

```bash
# 备份数据库
docker exec langtou-mysql-master mysqldump -u root -p langtou > langtou_backup_$(date +%Y%m%d).sql

# 备份配置文件
tar -czf langtou-config-backup_$(date +%Y%m%d).tar.gz k8s/ nginx/ monitoring/

# 备份数据卷
docker run --rm -v langtou_mysql-master-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup_$(date +%Y%m%d).tar.gz -C /data .
```

### 升级

```bash
# 1. 更新镜像标签
# 修改 .env 中的 IMAGE_TAG

# 2. 拉取新镜像并重启
docker compose pull
docker compose up -d

# 3. 或滚动更新单个服务
docker compose up -d --no-deps --build gateway

# 4. 验证
./scripts/stop-all.sh status
```

## License

Copyright (c) 2024 Langtou Team. All rights reserved.
