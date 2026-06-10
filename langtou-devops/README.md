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
    ├── stop-all.sh          # 一键停止
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
│    ┌─────────────┐    ┌─────────────┐                                 │
│    │   Kafka     │    │   Nacos     │                                 │
│    │  (消息队列)  │    │ (注册中心)   │                                 │
│    └─────────────┘    └─────────────┘                                 │
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

## 快速开始

### 前置要求

- Docker 24.x+
- Docker Compose 2.x+
- Kubernetes 1.28+ (生产部署)
- Jenkins 2.x (CI/CD)

### 1. 开发环境快速启动

```bash
cd /workspace/langtou-devops

# 启动开发环境基础服务
docker compose -f docker-compose.dev.yml up -d

# 查看服务状态
docker compose ps
```

### 2. 生产环境启动

```bash
cd /workspace/langtou-devops

# 初始化数据库
./scripts/init-db.sh

# 一键启动所有服务
chmod +x ./scripts/*.sh
./scripts/start-all.sh

# 或分步启动
./scripts/start-all.sh infra    # 启动基础设施
./scripts/start-all.sh monitoring # 启动监控
./scripts/start-all.sh services  # 启动业务服务
```

### 3. Kubernetes 部署

```bash
cd /workspace/langtou-devops

# 部署到 Kubernetes
chmod +x ./scripts/deploy-k8s.sh
./scripts/deploy-k8s.sh dev v1.0.0

# 滚动更新
./scripts/deploy-k8s.sh update gateway v1.0.1

# 回滚
./scripts/deploy-k8s.sh rollback gateway
```

## 服务端口

| 服务 | 端口 | 描述 |
|------|------|------|
| Gateway | 8080 | API 网关 |
| Nacos | 8848 | 注册中心/配置中心 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
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
│ 镜像构建        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Push to Registry│
│ 推送镜像        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ K8s Deploy      │
│ 部署            │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Health Check    │
│ 健康检查        │
└─────────────────┘
```

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
DOCKER_REGISTRY=registry.langtou.local
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

## 常见问题

### Q: Redis Cluster 初始化失败?

```bash
# 手动初始化
docker exec -it langtou-redis-1 redis-cli --cluster create \
  172.20.0.20:6379 172.20.0.21:6380 172.20.0.22:6381 \
  172.20.0.23:6382 172.20.0.24:6383 172.20.0.25:6384 \
  --cluster-replicas 1 -a Langtou@Redis2024
```

### Q: Nacos 无法连接 MySQL?

```bash
# 确保 MySQL 完全启动后再启动 Nacos
docker compose up -d mysql-master
sleep 30
docker compose up -d nacos
```

### Q: 如何查看服务日志?

```bash
# 查看所有服务日志
./scripts/stop-all.sh logs

# 查看指定服务日志
./scripts/stop-all.sh logs gateway
```

### Q: 如何清理所有数据?

```bash
./scripts/stop-all.sh clean
```

## 维护

### 备份

```bash
# 备份数据库
docker exec langtou-mysql-master mysqldump -u root -p langtou > langtou_backup.sql

# 备份配置文件
tar -czf langtou-config-backup.tar.gz k8s/ nginx/ monitoring/
```

### 升级

```bash
# 1. 更新镜像标签
# 2. 滚动更新
./scripts/deploy-k8s.sh update gateway v1.1.0

# 3. 验证
./scripts/deploy-k8s.sh status
```

## License

Copyright (c) 2024 Langtou Team. All rights reserved.
