# Langtou 社交内容社区 - 生产环境部署指南

## 目录

1. [环境要求](#1-环境要求)
2. [部署架构概览](#2-部署架构概览)
3. [部署前准备](#3-部署前准备)
4. [部署步骤](#4-部署步骤)
5. [验证检查](#5-验证检查)
6. [回滚流程](#6-回滚流程)
7. [灰度发布流程](#7-灰度发布流程)
8. [备份与恢复](#8-备份与恢复)
9. [常见问题排查](#9-常见问题排查)
10. [运维操作手册](#10-运维操作手册)

---

## 1. 环境要求

### 1.1 硬件要求

| 组件 | 最低配置 | 推荐配置 | 说明 |
|------|----------|----------|------|
| Kubernetes Master | 4C8G | 8C16G x 3 | 高可用部署至少3节点 |
| Kubernetes Worker | 8C16G | 16C32G x 3+ | 根据服务规模扩展 |
| MySQL | 4C8G | 8C32G | 主从部署 |
| Redis | 4C8G | 8C16G | 哨兵/集群模式 |
| Elasticsearch | 4C16G | 8C32G x 3 | 3节点集群 |
| Kafka | 4C8G | 8C16G x 3 | 3节点集群 |
| Nginx | 2C4G | 4C8G x 2 | 主备部署 |

### 1.2 软件要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| Kubernetes | >= 1.24 | 推荐 1.28+ |
| Docker | >= 23.0 | 推荐 24.0+ |
| Helm | >= 3.10 | 包管理 |
| Nginx Ingress | >= 1.5 | 流量入口 |
| cert-manager | >= 1.12 | TLS证书管理 |
| Argo Rollouts | >= 1.4 | 金丝雀部署 (可选) |
| Prometheus | >= 2.45 | 监控 |
| Grafana | >= 9.5 | 监控面板 |
| Java | JDK 17/21 | 服务运行时 |
| Node.js | >= 18 | 前端构建 |

### 1.3 网络要求

| 端口 | 用途 | 说明 |
|------|------|------|
| 80 | HTTP | 自动跳转HTTPS |
| 443 | HTTPS | 主服务入口 |
| 8080 | API Gateway | 内部服务端口 |
| 3306 | MySQL | 数据库 |
| 6379 | Redis | 缓存 |
| 9200 | Elasticsearch | 搜索引擎 |
| 9092 | Kafka | 消息队列 |

---

## 2. 部署架构概览

```
                    +------------------+
                    |   CDN / WAF      |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  Nginx Ingress   |
                    |  (HTTPS/限流)    |
                    +--------+---------+
                             |
                    +--------v---------+
                    |   API Gateway    |
                    | (路由/鉴权/限流)  |
                    +--------+---------+
                             |
        +----------+---------+---------+----------+
        |          |         |         |          |
   +----v---+ +---v----+ +--v-----+ +v-------+ +v-------+
   | User   | |Content | |Interact| |Message | |Search  |
   |Service | |Service | |Service | |Service | |Service |
   +----+---+ +---+----+ +--+-----+ +---+----+ +---+----+
        |         |         |          |           |
   +----v---------v---------v----------v-----------v----+
   |         MySQL    Redis    Kafka    ES             |
   +---------------------------------------------------+
```

---

## 3. 部署前准备

### 3.1 基础设施准备

```bash
# 1. 创建 Kubernetes 命名空间
kubectl apply -f langtou-devops/k8s/namespace.yml

# 2. 创建 ServiceAccount 和 RBAC
kubectl apply -f langtou-devops/k8s/rbac/service-account.yml

# 3. 创建 ConfigMap
kubectl apply -f langtou-devops/k8s/configmap.yml

# 4. 创建 Secret (请修改为实际值)
kubectl apply -f langtou-devops/k8s/secret.yml

# 5. 创建网络策略
kubectl apply -f langtou-devops/k8s/network/network-policy.yml
```

### 3.2 配置检查

```bash
# 检查 .env 文件配置
cat langtou-devops/.env

# 确认以下配置项已正确设置:
# - 数据库连接信息
# - Redis 连接信息
# - JWT 密钥
# - Docker 镜像仓库地址
# - 域名和证书信息
```

### 3.3 安全检查

```bash
# 执行安全扫描
bash langtou-devops/scripts/security-scan.sh --full

# 检查安全清单
# 确认 langtou-devops/security/security-checklist.md 中所有项已通过
```

---

## 4. 部署步骤

### 4.1 基础设施部署 (StatefulSet)

```bash
# 1. 部署 MySQL
kubectl apply -f langtou-devops/k8s/statefulset/mysql-statefulset.yml
kubectl rollout status statefulset/mysql -n langtou --timeout=300s

# 2. 部署 Redis
kubectl apply -f langtou-devops/k8s/statefulset/redis-statefulset.yml
kubectl rollout status statefulset/redis -n langtou --timeout=300s

# 3. 部署 Elasticsearch
kubectl apply -f langtou-devops/k8s/statefulset/es-statefulset.yml
kubectl rollout status statefulset/es-cluster -n langtou --timeout=600s

# 4. 部署 Kafka
kubectl apply -f langtou-devops/k8s/statefulset/kafka-statefulset.yml
kubectl rollout status statefulset/kafka -n langtou --timeout=300s
```

### 4.2 数据库初始化

```bash
# 执行数据库初始化脚本
bash langtou-devops/scripts/init-db.sh
```

### 4.3 业务服务部署

```bash
# 1. 部署各微服务
kubectl apply -f langtou-devops/k8s/services/user-service.yml
kubectl apply -f langtou-devops/k8s/services/content-service.yml
kubectl apply -f langtou-devops/k8s/services/interact-service.yml
kubectl apply -f langtou-devops/k8s/services/message-service.yml

# 2. 等待所有服务就绪
kubectl rollout status deployment/user-service -n langtou --timeout=300s
kubectl rollout status deployment/content-service -n langtou --timeout=300s
kubectl rollout status deployment/interact-service -n langtou --timeout=300s
kubectl rollout status deployment/message-service -n langtou --timeout=300s

# 3. 部署 API Gateway
kubectl apply -f langtou-devops/k8s/services/gateway.yml
kubectl rollout status deployment/langtou-gateway -n langtou --timeout=300s
```

### 4.4 Ingress 和前端部署

```bash
# 1. 部署 Ingress
kubectl apply -f langtou-devops/k8s/ingress/ingress.yml

# 2. 部署 Nginx 配置 (CDN等)
# 确保 cdn.conf 已包含在 nginx.conf 中

# 3. 配置 TLS 证书
# 方式1: Let's Encrypt (推荐)
kubectl apply -f langtou-devops/k8s/cert-manager.yml

# 方式2: 自签名证书 (仅测试)
kubectl create secret tls langtou-tls \
    --cert=ssl/langtou.crt \
    --key=ssl/langtou.key \
    -n langtou
```

### 4.5 监控部署

```bash
# 部署 Prometheus + Grafana + Loki
kubectl apply -f langtou-devops/monitoring/
```

### 4.6 一键部署

```bash
# 使用一键部署脚本
bash langtou-devops/scripts/deploy-k8s.sh
```

---

## 5. 验证检查

### 5.1 服务健康检查

```bash
# 检查所有 Pod 状态
kubectl get pods -n langtou -o wide

# 确认所有 Pod 为 Running 状态
kubectl get pods -n langtou --field-selector=status.phase=Running

# 检查 Service 端点
kubectl get endpoints -n langtou

# 检查 HPA 状态
kubectl get hpa -n langtou
```

### 5.2 API 接口验证

```bash
# 健康检查
curl -s https://api.langtou.com/health
# 预期: 200 "healthy"

# API Gateway 检查
curl -s https://api.langtou.com/api/actuator/health | jq .

# 用户注册测试
curl -X POST https://api.langtou.com/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","password":"Test@12345","email":"test@langtou.com"}' | jq .

# 用户登录测试
curl -X POST https://api.langtou.com/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","password":"Test@12345"}' | jq .
```

### 5.3 数据库验证

```bash
# MySQL 连接测试
kubectl exec -it mysql-0 -n langtou -- mysql -u root -p -e "SHOW DATABASES;"

# Redis 连接测试
kubectl exec -it redis-0 -n langtou -- redis-cli ping

# ES 集群状态
curl -s http://es-cluster-langtou:9200/_cluster/health?pretty
```

### 5.4 性能验证

```bash
# 简单压力测试
ab -n 1000 -c 50 https://api.langtou.com/health

# 检查响应时间 < 100ms
# 检查错误率 = 0%
```

### 5.5 安全验证

```bash
# 检查 HTTPS 证书
curl -vI https://api.langtou.com 2>&1 | grep -E "SSL|TLS"

# 检查安全响应头
curl -sI https://api.langtou.com | grep -E "X-Frame|X-Content|Strict-Transport"

# 检查 Swagger 是否已关闭
curl -s https://api.langtou.com/swagger-ui.html
# 预期: 404
```

---

## 6. 回滚流程

### 6.1 Deployment 回滚

```bash
# 查看部署历史
kubectl rollout history deployment/user-service -n langtou

# 回滚到上一版本
kubectl rollout undo deployment/user-service -n langtou

# 回滚到指定版本
kubectl rollout undo deployment/user-service -n langtou --to-revision=2

# 查看回滚状态
kubectl rollout status deployment/user-service -n langtou
```

### 6.2 全量回滚 (紧急)

```bash
# 一键回滚所有服务到上一版本
for svc in user-service content-service interact-service message-service langtou-gateway; do
    echo "Rolling back ${svc}..."
    kubectl rollout undo deployment/${svc} -n langtou
    kubectl rollout status deployment/${svc} -n langtou --timeout=300s
done
```

### 6.3 数据库回滚

```bash
# 1. 停止应用服务
kubectl scale deployment user-service --replicas=0 -n langtou
kubectl scale deployment content-service --replicas=0 -n langtou

# 2. 恢复数据库备份
mysql -h <mysql-host> -u root -p langtou < /data/backups/mysql/langtou_mysql_database_YYYYMMDD.sql.gz

# 3. 重新启动服务
kubectl scale deployment user-service --replicas=2 -n langtou
kubectl scale deployment content-service --replicas=2 -n langtou
```

---

## 7. 灰度发布流程

### 7.1 金丝雀部署 (推荐)

```bash
# 使用自动化脚本
bash langtou-devops/scripts/canary-deploy.sh deploy user-service \
    registry.langtou.local/langtou/user-service:v2.0.0-canary

# 渐进式发布 (10% -> 30% -> 50% -> 100%)
bash langtou-devops/scripts/canary-deploy.sh rollout user-service

# 手动调整流量
bash langtou-devops/scripts/canary-deploy.sh weight user-service 30

# 全量发布
bash langtou-devops/scripts/canary-deploy.sh promote user-service

# 回滚金丝雀
bash langtou-devops/scripts/canary-deploy.sh rollback user-service
```

### 7.2 Argo Rollouts (可选)

```bash
# 如果安装了 Argo Rollouts，使用 Rollout 配置
kubectl apply -f langtou-devops/k8s/canary/canary-rollout.yml

# 查看发布状态
kubectl argo rollouts get rollouts user-service-rollout -n langtou

# 暂停/继续
kubectl argo rollouts pause user-service-rollout -n langtou
kubectl argo rollouts promote user-service-rollout -n langtou

# 中止并回滚
kubectl argo rollouts abort user-service-rollout -n langtou
```

---

## 8. 备份与恢复

### 8.1 定时备份配置

```bash
# 配置 crontab 定时备份
bash langtou-devops/scripts/backup/backup-cron.sh --setup-cron

# 手动执行全量备份
bash langtou-devops/scripts/backup/backup-cron.sh --all

# 单独备份
bash langtou-devops/scripts/backup/mysql-backup.sh --full
bash langtou-devops/scripts/backup/redis-backup.sh
bash langtou-devops/scripts/backup/es-backup.sh
```

### 8.2 备份恢复

```bash
# MySQL 恢复
gunzip < /data/backups/mysql/langtou_mysql_database_YYYYMMDD.sql.gz | \
    mysql -h <host> -u root -p langtou

# Redis 恢复
gunzip < /data/backups/redis/langtou_redis_YYYYMMDD.rdb.gz > /var/lib/redis/dump.rdb
systemctl restart redis

# ES 恢复
curl -X POST "http://<es-host>:9200/_snapshot/langtou_backup_repo/<snapshot_name>/_restore" \
    -u elastic:<password> -H "Content-Type: application/json" -d '{}'
```

---

## 9. 常见问题排查

### 9.1 Pod 无法启动

```bash
# 查看 Pod 事件
kubectl describe pod <pod-name> -n langtou

# 查看容器日志
kubectl logs <pod-name> -n langtou --tail=100

# 常见原因:
# - 镜像拉取失败: 检查 imagePullSecrets 和镜像仓库地址
# - 资源不足: 检查节点资源使用情况
# - 配置错误: 检查 ConfigMap/Secret 挂载
# - 健康检查失败: 检查 liveness/readiness probe 配置
```

### 9.2 服务间通信失败

```bash
# 检查 Service 端点
kubectl get endpoints <service-name> -n langtou

# 检查 DNS 解析
kubectl exec -it <pod-name> -n langtou -- nslookup user-service.langtou.svc.cluster.local

# 检查网络策略
kubectl get networkpolicy -n langtou

# 常见原因:
# - Service selector 不匹配
# - NetworkPolicy 阻断通信
# - DNS 解析异常
```

### 9.3 数据库连接失败

```bash
# 检查 MySQL Pod 状态
kubectl get pods -l app=mysql -n langtou

# 检查 MySQL 连接
kubectl exec -it mysql-0 -n langtou -- mysqladmin -u root -p ping

# 检查连接数
kubectl exec -it mysql-0 -n langtou -- mysql -u root -p -e "SHOW PROCESSLIST;"

# 常见原因:
# - 密码错误: 检查 Secret 配置
# - 连接数满: 调整 max_connections
# - 主从同步延迟: 检查 Slave_IO/Slave_SQL 状态
```

### 9.4 内存溢出 (OOM)

```bash
# 查看 OOM 事件
kubectl get events -n langtou --field-selector type=Warning | grep OOM

# 调整资源限制
kubectl edit deployment user-service -n langtou
# 修改 resources.limits.memory 和 resources.requests.memory

# 调整 JVM 参数
# 修改 JVM_OPTS: -Xms512m -Xmx1024m -> -Xms1g -Xmx2g
```

### 9.5 Nginx 502/504 错误

```bash
# 检查 Ingress Controller 日志
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=100

# 检查上游服务状态
kubectl get pods -n langtou

# 常见原因:
# - 上游服务未就绪
# - 超时配置过短
# - 负载均衡器配置错误
```

---

## 10. 运维操作手册

### 10.1 日常巡检

```bash
# 每日巡检脚本
echo "=== Pod 状态 ==="
kubectl get pods -n langtou

echo "=== 资源使用 ==="
kubectl top pods -n langtou --sort-by=cpu
kubectl top nodes

echo "=== 磁盘使用 ==="
df -h /data

echo "=== 最近错误事件 ==="
kubectl get events -n langtou --sort-by='.lastTimestamp' --field-selector type=Warning | tail -20
```

### 10.2 日志查看

```bash
# 查看服务日志
kubectl logs -f deployment/user-service -n langtou --tail=200

# 查看 Nginx 访问日志
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=100

# 查看 Grafana 日志 (Loki)
# 访问 Grafana Explore 页面查询日志
```

### 10.3 扩缩容

```bash
# 手动扩缩容
kubectl scale deployment user-service --replicas=4 -n langtou

# 查看 HPA 状态
kubectl get hpa -n langtou -w
```

### 10.4 配置更新

```bash
# 更新 ConfigMap
kubectl edit configmap langtou-service-config -n langtou

# 更新 Secret
kubectl edit secret langtou-db-secret -n langtou

# 重启服务使配置生效
kubectl rollout restart deployment/user-service -n langtou
```

---

**文档版本**: v1.0
**最后更新**: 2026-06-11
**维护人**: Langtou DevOps Team
