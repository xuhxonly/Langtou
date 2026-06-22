# 榔头项目本地开发启动指南

本文档提供了在本地开发环境（IDE + 本地中间件）启动榔头项目的详细步骤。

## 1. 环境准备

确保您的本地环境已安装以下工具：
- **JDK 17+**
- **Maven 3.8+**
- **Node.js 18+**
- **Docker & Docker Compose**
- **MySQL 8.0 Client** (或 Navicat/DBeaver 等 GUI 工具)

## 2. 启动基础设施

榔头项目依赖 MySQL, Redis, Nacos, Elasticsearch 等中间件。为了方便本地开发，我们提供了一个仅包含基础设施的 Docker Compose 文件。

### 步骤 2.1：创建基础设施 Compose 文件
在 `langtou-devops` 目录下创建一个名为 `docker-compose.infra.yml` 的文件，内容如下：

```yaml
# 基础设施配置 (MySQL, Redis, Nacos, ES, MinIO)
version: "3.9"

networks:
  langtou-infra-network:
    driver: bridge

volumes:
  mysql-infra-data:
  redis-infra-data:
  nacos-infra-data:
  es-infra-data:
  minio-infra-data:

services:
  # MySQL 8.0
  mysql:
    image: mysql:8.0
    container_name: langtou-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: langtou
      MYSQL_USER: admin
      MYSQL_PASSWORD: password
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-infra-data:/var/lib/mysql
    networks:
      - langtou-infra-network
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci

  # Redis
  redis:
    image: redis:7-alpine
    container_name: langtou-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis-infra-data:/data
    networks:
      - langtou-infra-network

  # Nacos (Standalone)
  nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: langtou-nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "false"
      TZ: Asia/Shanghai
    ports:
      - "8848:8848"
      - "9848:9848"
    volumes:
      - nacos-infra-data:/home/nacos/data
    networks:
      - langtou-infra-network

  # Elasticsearch (Single Node)
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.12.0
    container_name: langtou-es
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - TZ=Asia/Shanghai
    ports:
      - "9200:9200"
    volumes:
      - es-infra-data:/usr/share/elasticsearch/data
    networks:
      - langtou-infra-network

  # MinIO (Object Storage)
  minio:
    image: minio/minio:RELEASE.2024-05-01T01-11-10Z
    container_name: langtou-minio
    restart: unless-stopped
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
      TZ: Asia/Shanghai
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-infra-data:/data
    networks:
      - langtou-infra-network
    command: server /data --console-address ":9001"
```

### 步骤 2.2：启动基础设施
在 `langtou-devops` 目录下执行：

```bash
docker compose -f docker-compose.infra.yml up -d
```

等待所有容器健康运行。

## 3. 初始化数据库

### 步骤 3.1：连接 MySQL
使用客户端连接 MySQL：
- Host: `localhost`
- Port: `3306`
- User: `root`
- Password: `root`

### 步骤 3.2：执行 SQL 脚本
按以下顺序执行 `langtou-database/flyway/migrations/` 下的 SQL 脚本（或使用 Flyway 自动执行）：

1. `V1__init_schema.sql` (初始化表结构)
2. `V2__init_data.sql` (初始化数据)
3. `V20__quiz_mvp.sql` (Quiz MVP 表结构)
4. `V21__note_quiz_extension.sql` (笔记关联 Quiz)
5. `V22__quiz_fixes.sql` (Quiz 修复)
6. `V23__create_user_skill_profile.sql` (AI 画像)
7. `V24__create_creative_and_connection.sql` (AI 创意)

或者，可以直接在数据库中执行 `langtou-database/schema.sql` 和 `langtou-database/data.sql`（如果有整合版本）。

## 4. 启动后端服务

推荐使用 IntelliJ IDEA 逐个启动以下服务：

| 服务 | 入口类 | 端口 | 启动顺序 |
|------|--------|------|---------|
| **langtou-gateway** | `GatewayApplication` | 8080 | 4 (最后启动) |
| **langtou-user-service** | `UserServiceApplication` | 8081 | 1 |
| **langtou-content-service** | `ContentServiceApplication` | 8082 | 1 |
| **langtou-quiz-service** | `QuizServiceApplication` | 8089 | 2 |
| **langtou-ai-service** | `AiServiceApplication` | 8090 | 2 |

**注意事项：**
- 每个服务的 `application.yml` 中配置的 Nacos 地址为 `localhost:8848`。
- 确保 MySQL, Redis, Nacos 的连接配置正确。

## 5. 启动前端应用

### 移动端 (React Native)
```bash
cd langtou-mobile/LangtouMobile
npm install
npm start
```

### 后台管理 (Web)
```bash
cd langtou-admin
npm install
npm run dev
```

## 6. 验证服务

启动完成后，可以通过以下方式验证：

1. **Nacos 控制台**: http://localhost:8848/nacos (查看服务注册情况)
2. **Swagger UI**: 
   - User Service: http://localhost:8081/swagger-ui.html
   - Quiz Service: http://localhost:8089/swagger-ui.html
3. **API 测试**: 通过 Gateway (http://localhost:8080) 调用接口

## 常见问题

**Q: 启动时提示数据库表不存在？**
A: 请检查是否已执行所有的 Flyway 迁移脚本。

**Q: 服务启动失败，提示连接 Nacos 超时？**
A: 请确保 Nacos 容器已启动并运行正常。可以访问 http://localhost:8848/nacos 验证。

**Q: 如何查看各个服务的日志？**
A: 在 IDEA 的 Run 窗口或 `docker compose logs -f <service-name>` 查看。