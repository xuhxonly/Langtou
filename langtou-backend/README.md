# Langtou Backend

榔头(Langtou)社交内容社区APP后端微服务架构

## 技术栈

- Java 17
- Spring Boot 3.2.x
- Spring Cloud 2023.0.x
- MyBatis-Plus 3.5.x
- JWT (jjwt 0.12.x)
- MySQL 8.x
- Redis
- Maven

## 项目结构

```
langtou-backend/
├── pom.xml                              # 根项目POM
├── langtou-common/                      # 公共模块
│   ├── src/main/java/com/langtou/common/
│   │   ├── result/
│   │   │   ├── Result.java              # 统一响应封装
│   │   │   └── ResultCode.java          # 响应状态码枚举
│   │   ├── exception/
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   └── GlobalExceptionHandler.java # 全局异常处理
│   │   ├── utils/
│   │   │   ├── JwtUtils.java            # JWT工具类
│   │   │   └── PageUtils.java           # 分页工具类
│   │   └── constant/
│   │       └── CommonConstants.java     # 常量定义
│   └── pom.xml
├── langtou-gateway/                     # API网关
│   ├── src/main/java/com/langtou/gateway/
│   │   ├── GatewayApplication.java
│   │   ├── filter/
│   │   │   └── JwtAuthFilter.java       # JWT鉴权过滤器
│   │   └── config/
│   │       └── RateLimiterConfig.java   # 限流配置
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
├── langtou-user-service/                # 用户服务 (端口: 8081)
│   ├── src/main/java/com/langtou/user/
│   │   ├── UserServiceApplication.java
│   │   ├── controller/UserController.java
│   │   ├── service/UserService.java
│   │   ├── service/impl/UserServiceImpl.java
│   │   ├── mapper/UserMapper.java
│   │   ├── entity/User.java
│   │   └── dto/
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
├── langtou-content-service/             # 内容服务 (端口: 8082)
│   ├── src/main/java/com/langtou/content/
│   │   ├── ContentServiceApplication.java
│   │   ├── controller/ContentController.java
│   │   ├── service/ContentService.java
│   │   ├── service/impl/ContentServiceImpl.java
│   │   ├── mapper/ContentMapper.java
│   │   ├── entity/Content.java
│   │   └── dto/ContentDTO.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
├── langtou-interact-service/            # 互动服务 (端口: 8083)
│   ├── src/main/java/com/langtou/interact/
│   │   ├── InteractServiceApplication.java
│   │   ├── controller/InteractController.java
│   │   ├── service/InteractService.java
│   │   ├── service/impl/InteractServiceImpl.java
│   │   ├── mapper/
│   │   ├── entity/
│   │   └── dto/
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
└── langtou-message-service/             # 消息服务 (端口: 8084)
    ├── src/main/java/com/langtou/message/
    │   ├── MessageServiceApplication.java
    │   ├── controller/MessageController.java
    │   ├── service/MessageService.java
    │   ├── service/impl/MessageServiceImpl.java
    │   ├── mapper/MessageMapper.java
    │   ├── entity/Message.java
    │   └── dto/
    ├── src/main/resources/application.yml
    ├── Dockerfile
    └── pom.xml
```

## 服务端口

| 服务 | 端口 |
|------|------|
| langtou-gateway | 8080 |
| langtou-user-service | 8081 |
| langtou-content-service | 8082 |
| langtou-interact-service | 8083 |
| langtou-message-service | 8084 |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 数据库初始化

创建以下数据库：

```sql
CREATE DATABASE langtou_user CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE langtou_content CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE langtou_interact CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE langtou_message CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 编译项目

```bash
# 在根目录执行
mvn clean install
```

### 启动服务

```bash
# 1. 启动用户服务
mvn -pl langtou-user-service spring-boot:run

# 2. 启动内容服务
mvn -pl langtou-content-service spring-boot:run

# 3. 启动互动服务
mvn -pl langtou-interact-service spring-boot:run

# 4. 启动消息服务
mvn -pl langtou-message-service spring-boot:run

# 5. 启动网关（最后启动）
mvn -pl langtou-gateway spring-boot:run
```

或者使用IDE分别运行各模块的 `*Application` 主类。

### Docker 构建

```bash
# 先编译
mvn clean package

# 构建镜像
docker build -t langtou-gateway ./langtou-gateway
docker build -t langtou-user-service ./langtou-user-service
docker build -t langtou-content-service ./langtou-content-service
docker build -t langtou-interact-service ./langtou-interact-service
docker build -t langtou-message-service ./langtou-message-service
```

## API 接口

### 用户服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/user/register | 用户注册 |
| POST | /api/user/login | 用户登录 |
| GET | /api/user/info/{id} | 获取用户信息 |
| GET | /api/user/me | 获取当前用户信息 |
| PUT | /api/user/me | 更新当前用户信息 |

### 内容服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/content/publish | 发布内容 |
| GET | /api/content/{id} | 获取内容详情 |
| GET | /api/content/user/{userId} | 获取用户内容列表 |
| DELETE | /api/content/{id} | 删除内容 |
| GET | /api/content/feed | 获取内容流 |

### 互动服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/interact/like/{contentId} | 点赞 |
| POST | /api/interact/unlike/{contentId} | 取消点赞 |
| GET | /api/interact/like/status/{contentId} | 点赞状态 |
| POST | /api/interact/comment/{contentId} | 评论 |
| GET | /api/interact/comment/{contentId} | 获取评论列表 |
| DELETE | /api/interact/comment/{commentId} | 删除评论 |

### 消息服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/message/send | 发送消息 |
| GET | /api/message/inbox | 收件箱 |
| GET | /api/message/conversation/{targetId} | 会话记录 |
| GET | /api/message/unread/count | 未读消息数 |
| POST | /api/message/read/{senderId} | 标记已读 |

## 认证方式

所有需要认证的接口需在请求头中携带：

```
Authorization: Bearer <token>
```

Token通过 `/api/user/login` 接口获取。

## 开发规范

- 统一返回格式：`Result<T>`
- 统一异常处理：`GlobalExceptionHandler`
- 统一常量定义：`CommonConstants`
- 统一分页工具：`PageUtils`
- 统一JWT工具：`JwtUtils`
