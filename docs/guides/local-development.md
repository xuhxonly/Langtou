# Langtou 本地开发指南

## 环境要求

- Java 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0+
- Redis 7+

## 快速启动

### 1. 启动基础设施

```bash
# 使用 Docker（推荐）
cd infrastructure/devops
docker compose -f docker-compose.local.yml up -d

# 或者手动安装
# MySQL: https://dev.mysql.com/downloads/installer/
# Redis: https://redis.io/download
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，修改敏感配置：

```bash
cp .env.example .env
# 编辑 .env 文件，设置密码等配置
```

### 3. 启动后端服务

```bash
cd src/backend

# 编译
mvn clean install -DskipTests

# 启动服务（按依赖顺序）
# 1. 公共模块
mvn install -pl langtou-common

# 2. 核心服务
cd langtou-user-service && mvn spring-boot:run
cd langtou-content-service && mvn spring-boot:run
cd langtou-interact-service && mvn spring-boot:run

# 3. 网关（最后启动）
cd langtou-gateway && mvn spring-boot:run
```

### 4. 启动前端服务

```bash
# Web 前端
cd src/frontend/web
npm install
npm run dev

# Admin 管理后台
cd src/frontend/admin
npm install
npm run dev
```

### 5. 访问服务

- 网关: http://localhost:8080
- Web: http://localhost:5173
- Admin: http://localhost:5174
- API 文档: http://localhost:8080/swagger-ui.html

## 使用 H2 内存数据库（无需 MySQL）

如果没有 MySQL，可以使用 H2 内存数据库进行本地开发：

### 1. 创建本地配置文件

为每个服务创建 `application-local.yml`：

```yaml
# src/backend/<service>/src/main/resources/application-local.yml
server:
  port: 8081

spring:
  application:
    name: langtou-user-service
  datasource:
    url: jdbc:h2:mem:langtou_user;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
      path: /h2-console
  cloud:
    nacos:
      discovery:
        enabled: false

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.langtou.user.entity
  configuration:
    map-underscore-to-camel-case: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### 2. 添加 H2 依赖

在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 3. 启动服务

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. 访问 H2 控制台

http://localhost:8081/h2-console

## 常见问题

### Q: 启动时提示数据库连接失败？
A: 请确保 MySQL 已启动，或使用 H2 内存数据库模式。

### Q: 服务注册失败？
A: 请确保 Nacos 已启动，或在本地配置中禁用 Nacos。

### Q: 端口被占用？
A: 修改 `application.yml` 中的 `server.port` 配置。

## 开发规范

### 代码提交

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
feat: 新功能
fix: Bug 修复
docs: 文档更新
style: 代码格式
refactor: 代码重构
test: 添加测试
chore: 构建/工具
```

### 分支管理

- `main`: 主分支，保持稳定
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 热修复分支

### 代码审查

- 所有 PR 必须经过代码审查
- 确保代码符合项目规范
- 添加必要的测试用例
