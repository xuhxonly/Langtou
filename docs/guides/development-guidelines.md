# 榔头 (Langtou) 项目开发规范

> 本文档定义了榔头(Langtou)全栈项目的统一开发规范，所有团队成员必须严格遵守。

---

## 1. 命名规范总览

| 类别 | 规范 | 示例 |
|------|------|------|
| 数据库表名 | 单数，下划线分隔 | `user`, `note_tag`, `like_record` |
| 数据库字段 | 下划线分隔，时间用 xxx_at | `user_id`, `created_at`, `is_read` |
| Java包名 | 全小写，模块名 | `com.langtou.user.service` |
| Java类名 | PascalCase | `UserService`, `NoteDetailVO` |
| Java方法名 | camelCase | `getFollowers`, `findByNoteId` |
| Java常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE`, `DEFAULT_AVATAR` |
| API路径 | `/api/v1/资源/动作` | `/api/v1/notes/{noteId}/like` |
| 前端组件 | PascalCase | `NoteCard`, `CommentItem` |
| 前端Hook | camelCase + use前缀 | `useNotes`, `useAuth` |
| 前端文件 | camelCase (组件除外) | `format.ts`, `useNotes.ts` |
| Python文件 | snake_case | `content_model.py`, `redis_client.py` |
| Docker服务名 | langtou- 前缀 + 小写 | `langtou-mysql`, `langtou-gateway` |
| K8s资源名 | langtou- 前缀 | `langtou-user-service` |

---

## 2. API路径规范

### 统一前缀
所有API以 `/api/v1` 开头。

### 路径格式
```
/api/v1/{资源}/{动作}
```

### 完整路径表

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 认证 | POST | `/api/v1/auth/login` | 登录 |
| 认证 | POST | `/api/v1/auth/register` | 注册 |
| 认证 | POST | `/api/v1/auth/refresh` | 刷新Token |
| 用户 | GET | `/api/v1/users/{userId}` | 获取用户信息 |
| 用户 | GET | `/api/v1/users/me` | 当前用户信息 |
| 用户 | PUT | `/api/v1/users/me/profile` | 修改资料 |
| 用户 | POST | `/api/v1/users/me/avatar` | 上传头像 |
| 关注 | POST | `/api/v1/users/{userId}/follow` | 关注 |
| 关注 | DELETE | `/api/v1/users/{userId}/follow` | 取消关注 |
| 关注 | GET | `/api/v1/users/{userId}/followers` | 粉丝列表 |
| 关注 | GET | `/api/v1/users/{userId}/following` | 关注列表 |
| 笔记 | GET | `/api/v1/notes` | Feed流(分页) |
| 笔记 | GET | `/api/v1/notes/{noteId}` | 笔记详情 |
| 笔记 | POST | `/api/v1/notes` | 发布笔记 |
| 笔记 | PUT | `/api/v1/notes/{noteId}` | 编辑笔记 |
| 笔记 | DELETE | `/api/v1/notes/{noteId}` | 删除笔记 |
| 笔记 | GET | `/api/v1/notes/user/{userId}` | 用户笔记 |
| 笔记 | POST | `/api/v1/notes/upload` | 上传图片/视频 |
| 点赞 | POST | `/api/v1/notes/{noteId}/like` | 点赞 |
| 点赞 | DELETE | `/api/v1/notes/{noteId}/like` | 取消点赞 |
| 点赞 | GET | `/api/v1/notes/{noteId}/like/status` | 点赞状态 |
| 收藏 | POST | `/api/v1/notes/{noteId}/collect` | 收藏 |
| 收藏 | DELETE | `/api/v1/notes/{noteId}/collect` | 取消收藏 |
| 收藏 | GET | `/api/v1/users/me/collections` | 收藏列表 |
| 评论 | GET | `/api/v1/notes/{noteId}/comments` | 评论列表 |
| 评论 | POST | `/api/v1/notes/{noteId}/comments` | 发表评论 |
| 评论 | POST | `/api/v1/comments/{commentId}/reply` | 回复评论 |
| 评论 | POST | `/api/v1/comments/{commentId}/like` | 点赞评论 |
| 分享 | POST | `/api/v1/notes/{noteId}/share` | 转发 |
| 标签 | GET | `/api/v1/tags/hot` | 热门标签 |
| 搜索 | GET | `/api/v1/search?keyword=&type=` | 搜索 |
| 消息 | GET | `/api/v1/messages/conversations` | 会话列表 |
| 消息 | GET | `/api/v1/messages/conversation/{userId}` | 聊天记录 |
| 消息 | POST | `/api/v1/messages/send` | 发送消息 |
| 消息 | PUT | `/api/v1/messages/conversation/{userId}/read` | 标记已读 |
| 通知 | GET | `/api/v1/notifications` | 通知列表 |
| 通知 | GET | `/api/v1/notifications/unread-count` | 未读数 |
| 通知 | PUT | `/api/v1/notifications/read-all` | 全部已读 |

---

## 3. 数据库规范

### 表命名
- 单数形式：`user`, `note`, `tag`
- 关联表：`note_tag`, `like_record`
- 不使用前缀区分模块（MVP阶段单库）

### 字段命名
- 主键：`id` (BIGINT UNSIGNED AUTO_INCREMENT)
- 外键：`xxx_id` (user_id, note_id)
- 布尔：`is_xxx` (is_read, is_deleted)
- 时间：`xxx_at` (created_at, updated_at)
- 计数：`xxx_count` (like_count, follower_count)
- 状态：`status` (TINYINT)
- 类型：`xxx_type` (target_type, message_type)

### 索引命名
- 普通索引：`idx_表名_字段名`
- 唯一索引：`uk_表名_字段名`

### 字符集
- 统一使用 `utf8mb4` + `utf8mb4_unicode_ci`

---

## 4. 后端Java规范

### 包结构
```
com.langtou.{module}/
├── controller/    # REST控制器
├── service/       # 服务接口
│   └── impl/      # 服务实现
├── mapper/        # MyBatis Mapper
├── entity/        # 数据库实体
├── dto/           # 入参对象
├── vo/            # 出参对象
├── config/        # 服务配置
├── handler/       # 处理器
└── listener/      # 事件监听
```

### DTO/VO区分
- **DTO (Data Transfer Object)**：入参，用于接收前端请求
- **VO (View Object)**：出参，用于返回前端响应

### 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误码分段
- 1000-1999：通用错误
- 2000-2999：用户模块
- 3000-3999：内容模块
- 4000-4999：互动模块
- 5000-5999：消息模块

---

## 5. 前端React Native规范

### 目录结构
```
src/
├── api/           # API请求（按领域划分）
├── components/    # 组件
│   ├── common/    # 通用组件
│   ├── note/      # 笔记组件
│   ├── home/      # 首页组件
│   └── profile/   # 个人中心组件
├── screens/       # 页面（每个页面一个文件夹）
├── hooks/         # 自定义Hooks
├── store/         # Zustand状态管理
├── types/         # TypeScript类型
├── utils/         # 工具函数
├── constants/     # 常量
└── assets/        # 静态资源
```

### API路径常量化
所有API路径定义在 `constants/api.ts` 的 `API_PATHS` 对象中，不在API函数中硬编码。

### 状态管理
- **服务端状态**：@tanstack/react-query
- **客户端状态**：Zustand

---

## 6. DevOps规范

### Docker服务命名
所有Docker服务名统一使用 `langtou-` 前缀。

### 环境变量
所有敏感信息通过 `.env` 文件管理，不硬编码在配置文件中。占位符使用 `CHANGE_ME`。

### 端口分配
| 服务 | 端口 |
|------|------|
| Nginx | 80/443 |
| Gateway | 8080 |
| User Service | 8081 |
| Content Service | 8082 |
| Interact Service | 8083 |
| Message Service | 8084 |
| Recommendation | 8000 |
| MySQL | 3306 |
| Redis | 6379 |
| Kafka | 9092 |
| Elasticsearch | 9200 |
| Nacos | 8848 |
| Prometheus | 9090 |
| Grafana | 3000 |

---

## 7. Git规范

### 分支策略
- `main`：生产分支
- `develop`：开发分支
- `feature/*`：功能分支
- `hotfix/*`：紧急修复

### Commit格式
```
<type>(<scope>): <subject>

type: feat/fix/refactor/docs/style/test/chore
scope: backend/frontend/database/devops/all
```

---

## 8. 安全规范

- JWT密钥通过配置文件管理，不硬编码
- 数据库密码通过环境变量管理
- API接口统一鉴权（网关JWT过滤器）
- 敏感接口限流
- SQL参数化查询，防止注入
- 文件上传类型和大小限制
