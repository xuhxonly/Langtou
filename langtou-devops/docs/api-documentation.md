# Langtou 社交内容社区 - API 文档

## 目录

1. [认证接口](#1-认证接口)
2. [用户接口](#2-用户接口)
3. [内容接口](#3-内容接口)
4. [互动接口](#4-互动接口)
5. [消息接口](#5-消息接口)
6. [搜索接口](#6-搜索接口)
7. [文件上传接口](#7-文件上传接口)
8. [错误码说明](#8-错误码说明)

---

## 通用说明

### 基础路径

```
生产环境: https://api.langtou.com
测试环境: http://dev-api.langtou.com
```

### 通用请求头

| Header | 必填 | 说明 |
|--------|------|------|
| Content-Type | 是 | application/json |
| Authorization | 是 (需认证接口) | Bearer {JWT Token} |
| X-Request-ID | 否 | 请求追踪ID |
| X-Client-Version | 否 | 客户端版本号 |

### 通用响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {},
    "timestamp": 1718000000000,
    "requestId": "uuid"
}
```

### 分页响应格式

```json
{
    "code": 200,
    "message": "success",
    "data": {
        "list": [],
        "total": 100,
        "page": 1,
        "pageSize": 20,
        "totalPages": 5
    }
}
```

---

## 1. 认证接口

### 1.1 用户注册

**POST** `/api/user/register`

请求参数:
```json
{
    "username": "langtou_user",
    "password": "MyPass@12345",
    "confirmPassword": "MyPass@12345",
    "email": "user@langtou.com",
    "phone": "13800138000",
    "captchaCode": "123456",
    "captchaKey": "uuid-key"
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "userId": 10001,
        "username": "langtou_user"
    }
}
```

错误响应:
| code | 说明 |
|------|------|
| 400 | 参数校验失败 |
| 409 | 用户名/邮箱/手机号已存在 |
| 422 | 验证码错误或过期 |

### 1.2 用户登录

**POST** `/api/user/login`

请求参数:
```json
{
    "username": "langtou_user",
    "password": "MyPass@12345",
    "loginType": "PASSWORD"
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 7200,
        "user": {
            "userId": 10001,
            "username": "langtou_user",
            "nickname": "榔头用户",
            "avatar": "https://cdn.langtou.com/images/avatar/10001.jpg",
            "email": "u***@langtou.com"
        }
    }
}
```

### 1.3 刷新Token

**POST** `/api/user/refresh-token`

请求参数:
```json
{
    "refreshToken": "eyJhbGciOiJSUzI1NiJ9..."
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "Token 刷新成功",
    "data": {
        "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJSUzI1NiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 7200
    }
}
```

### 1.4 用户登出

**POST** `/api/user/logout`

请求头: `Authorization: Bearer {token}`

成功响应 (200):
```json
{
    "code": 200,
    "message": "登出成功",
    "data": null
}
```

### 1.5 发送验证码

**POST** `/api/user/send-captcha`

请求参数:
```json
{
    "type": "REGISTER",
    "target": "13800138000",
    "captchaType": "SMS"
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "验证码已发送",
    "data": {
        "captchaKey": "uuid-key",
        "expiresIn": 300
    }
}
```

---

## 2. 用户接口

### 2.1 获取用户信息

**GET** `/api/user/{userId}`

请求头: `Authorization: Bearer {token}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "userId": 10001,
        "username": "langtou_user",
        "nickname": "榔头用户",
        "avatar": "https://cdn.langtou.com/images/avatar/10001.jpg",
        "bio": "这是我的个人简介",
        "gender": 1,
        "birthday": "2000-01-01",
        "location": "北京",
        "website": "https://langtou.com",
        "followerCount": 1200,
        "followingCount": 300,
        "postCount": 56,
        "isFollowed": false,
        "createdAt": "2024-01-01T00:00:00"
    }
}
```

### 2.2 更新用户信息

**PUT** `/api/user/profile`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "nickname": "新昵称",
    "bio": "新简介",
    "gender": 1,
    "birthday": "2000-01-01",
    "location": "上海"
}
```

### 2.3 关注/取关用户

**POST** `/api/user/{userId}/follow`

请求头: `Authorization: Bearer {token}`

成功响应 (200):
```json
{
    "code": 200,
    "message": "关注成功",
    "data": null
}
```

### 2.4 获取关注列表

**GET** `/api/user/{userId}/followers?page=1&pageSize=20`

请求头: `Authorization: Bearer {token}`

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "list": [
            {
                "userId": 10002,
                "username": "user2",
                "nickname": "用户2",
                "avatar": "https://cdn.langtou.com/images/avatar/10002.jpg",
                "isFollowed": true
            }
        ],
        "total": 1200,
        "page": 1,
        "pageSize": 20
    }
}
```

### 2.5 获取关注中列表

**GET** `/api/user/{userId}/following?page=1&pageSize=20`

请求头: `Authorization: Bearer {token}`

响应格式同关注列表。

---

## 3. 内容接口

### 3.1 发布帖子

**POST** `/api/content/post`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "title": "帖子标题",
    "content": "帖子内容（支持Markdown）",
    "contentHtml": "<p>HTML内容</p>",
    "images": [
        "https://cdn.langtou.com/images/post/202406/abc123.jpg"
    ],
    "videos": [],
    "tags": ["技术", "分享"],
    "categoryId": 1,
    "location": "北京",
    "visibility": "PUBLIC"
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "发布成功",
    "data": {
        "postId": 20001,
        "createdAt": "2024-06-11T10:00:00"
    }
}
```

### 3.2 获取帖子详情

**GET** `/api/content/post/{postId}`

请求头: `Authorization: Bearer {token}` (可选，未登录也可查看公开帖子)

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "postId": 20001,
        "title": "帖子标题",
        "content": "帖子内容",
        "contentHtml": "<p>HTML内容</p>",
        "images": ["https://cdn.langtou.com/images/post/202406/abc123.jpg"],
        "videos": [],
        "tags": ["技术", "分享"],
        "categoryId": 1,
        "categoryName": "技术交流",
        "author": {
            "userId": 10001,
            "username": "langtou_user",
            "nickname": "榔头用户",
            "avatar": "https://cdn.langtou.com/images/avatar/10001.jpg"
        },
        "likeCount": 50,
        "commentCount": 10,
        "shareCount": 5,
        "viewCount": 1000,
        "isLiked": false,
        "isBookmarked": false,
        "createdAt": "2024-06-11T10:00:00",
        "updatedAt": "2024-06-11T10:00:00"
    }
}
```

### 3.3 获取帖子列表 (Feed流)

**GET** `/api/content/feed?page=1&pageSize=20&type=RECOMMEND`

查询参数:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| pageSize | Integer | 否 | 每页数量，默认20，最大50 |
| type | String | 否 | RECOMMEND(推荐)/LATEST(最新)/FOLLOWING(关注) |
| categoryId | Long | 否 | 分类ID筛选 |
| tag | String | 否 | 标签筛选 |

### 3.4 删除帖子

**DELETE** `/api/content/post/{postId}`

请求头: `Authorization: Bearer {token}`

成功响应 (200):
```json
{
    "code": 200,
    "message": "删除成功",
    "data": null
}
```

### 3.5 获取分类列表

**GET** `/api/content/categories`

成功响应 (200):
```json
{
    "code": 200,
    "data": [
        {
            "categoryId": 1,
            "name": "技术交流",
            "icon": "https://cdn.langtou.com/images/category/tech.png",
            "postCount": 5000
        }
    ]
}
```

---

## 4. 互动接口

### 4.1 点赞

**POST** `/api/interact/like`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "targetType": "POST",
    "targetId": 20001
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "点赞成功",
    "data": null
}
```

### 4.2 取消点赞

**DELETE** `/api/interact/like`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "targetType": "POST",
    "targetId": 20001
}
```

### 4.3 评论

**POST** `/api/interact/comment`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "targetType": "POST",
    "targetId": 20001,
    "content": "评论内容",
    "parentId": 0,
    "replyToUserId": 0
}
```

成功响应 (200):
```json
{
    "code": 200,
    "message": "评论成功",
    "data": {
        "commentId": 30001,
        "createdAt": "2024-06-11T10:30:00"
    }
}
```

### 4.4 获取评论列表

**GET** `/api/interact/comments?targetType=POST&targetId=20001&page=1&pageSize=20`

请求头: `Authorization: Bearer {token}` (可选)

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "list": [
            {
                "commentId": 30001,
                "content": "评论内容",
                "author": {
                    "userId": 10002,
                    "nickname": "用户2",
                    "avatar": "https://cdn.langtou.com/images/avatar/10002.jpg"
                },
                "likeCount": 5,
                "isLiked": false,
                "replyCount": 2,
                "replies": [],
                "createdAt": "2024-06-11T10:30:00"
            }
        ],
        "total": 10,
        "page": 1,
        "pageSize": 20
    }
}
```

### 4.5 收藏

**POST** `/api/interact/bookmark`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "targetType": "POST",
    "targetId": 20001
}
```

### 4.6 分享

**POST** `/api/interact/share`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "targetType": "POST",
    "targetId": 20001,
    "shareType": "WECHAT"
}
```

---

## 5. 消息接口

### 5.1 获取消息列表

**GET** `/api/message/list?page=1&pageSize=20&type=SYSTEM`

查询参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| type | String | SYSTEM(系统)/COMMENT(评论)/LIKE(点赞)/FOLLOW(关注)/CHAT(聊天) |

### 5.2 获取未读消息数

**GET** `/api/message/unread-count`

请求头: `Authorization: Bearer {token}`

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "total": 15,
        "system": 3,
        "comment": 5,
        "like": 4,
        "follow": 2,
        "chat": 1
    }
}
```

### 5.3 标记消息已读

**PUT** `/api/message/read`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "type": "SYSTEM",
    "messageIds": [1001, 1002, 1003]
}
```

### 5.4 发送私信

**POST** `/api/message/chat`

请求头: `Authorization: Bearer {token}`

请求参数:
```json
{
    "toUserId": 10002,
    "content": "你好！",
    "messageType": "TEXT"
}
```

### 5.5 获取聊天记录

**GET** `/api/message/chat/{userId}?page=1&pageSize=50`

请求头: `Authorization: Bearer {token}`

---

## 6. 搜索接口

### 6.1 全文搜索

**GET** `/api/search?q=关键词&page=1&pageSize=20&type=ALL`

查询参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| q | String | 搜索关键词 (必填) |
| type | String | ALL(全部)/POST(帖子)/USER(用户)/TAG(标签) |
| page | Integer | 页码 |
| pageSize | Integer | 每页数量 |
| sort | String | RELEVANCE(相关度)/LATEST(最新)/HOT(热门) |

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "list": [
            {
                "id": 20001,
                "type": "POST",
                "title": "包含关键词的帖子标题",
                "content": "帖子摘要...",
                "author": {
                    "userId": 10001,
                    "nickname": "榔头用户",
                    "avatar": "https://cdn.langtou.com/images/avatar/10001.jpg"
                },
                "highlight": {
                    "title": "包含<em>关键词</em>的帖子标题",
                    "content": "帖子摘要中包含<em>关键词</em>..."
                },
                "score": 5.6,
                "createdAt": "2024-06-11T10:00:00"
            }
        ],
        "total": 50,
        "page": 1,
        "pageSize": 20
    }
}
```

### 6.2 热门搜索

**GET** `/api/search/hot`

成功响应 (200):
```json
{
    "code": 200,
    "data": [
        {"keyword": "榔头", "heat": 10000},
        {"keyword": "技术分享", "heat": 8000},
        {"keyword": "今日热点", "heat": 6000}
    ]
}
```

### 6.3 搜索建议

**GET** `/api/search/suggest?q=lang`

成功响应 (200):
```json
{
    "code": 200,
    "data": [
        {"type": "USER", "text": "langtou_user"},
        {"type": "TAG", "text": "langtou"},
        {"type": "POST", "text": "langtou项目介绍"}
    ]
}
```

---

## 7. 文件上传接口

### 7.1 上传图片

**POST** `/api/upload/image`

请求头:
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

请求参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| file | File | 图片文件 (支持 jpg/png/gif/webp, 最大10MB) |

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "fileId": "f_abc123",
        "url": "https://cdn.langtou.com/images/upload/202406/abc123.jpg",
        "thumbnailUrl": "https://cdn.langtou.com/images/upload/202406/abc123_thumb.jpg",
        "size": 102400,
        "width": 1920,
        "height": 1080,
        "mimeType": "image/jpeg"
    }
}
```

### 7.2 上传视频

**POST** `/api/upload/video`

请求头:
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

请求参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| file | File | 视频文件 (支持 mp4/mov/avi, 最大500MB) |

成功响应 (200):
```json
{
    "code": 200,
    "data": {
        "fileId": "v_xyz789",
        "url": "https://cdn.langtou.com/videos/upload/202406/xyz789.mp4",
        "coverUrl": "https://cdn.langtou.com/images/upload/202406/xyz789_cover.jpg",
        "size": 52428800,
        "duration": 120,
        "width": 1920,
        "height": 1080,
        "mimeType": "video/mp4"
    }
}
```

### 7.3 批量上传图片

**POST** `/api/upload/images`

请求头:
- `Authorization: Bearer {token}`
- `Content-Type: multipart/form-data`

请求参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| files | File[] | 图片文件数组 (最多9张) |

---

## 8. 错误码说明

### 8.1 通用错误码

| HTTP 状态码 | 业务码 | 说明 |
|-------------|--------|------|
| 200 | 200 | 成功 |
| 201 | 201 | 创建成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未认证 (未登录或Token过期) |
| 403 | 403 | 无权限 |
| 404 | 404 | 资源不存在 |
| 409 | 409 | 资源冲突 (已存在) |
| 422 | 422 | 业务校验失败 |
| 429 | 429 | 请求过于频繁 (限流) |
| 500 | 500 | 服务器内部错误 |
| 502 | 502 | 网关错误 |
| 503 | 503 | 服务不可用 |

### 8.2 业务错误码

| 业务码 | 说明 |
|--------|------|
| 10001 | 用户名已存在 |
| 10002 | 邮箱已注册 |
| 10003 | 手机号已注册 |
| 10004 | 用户名或密码错误 |
| 10005 | 账号已被禁用 |
| 10006 | 验证码错误 |
| 10007 | 验证码已过期 |
| 10008 | 密码不符合要求 |
| 10009 | Token 已过期 |
| 10010 | Token 无效 |
| 10011 | 账号异常，请重试 |

| 业务码 | 说明 |
|--------|------|
| 20001 | 帖子不存在 |
| 20002 | 帖子已被删除 |
| 20003 | 无权编辑此帖子 |
| 20004 | 内容包含敏感词 |
| 20005 | 发布频率过快 |
| 20006 | 分类不存在 |

| 业务码 | 说明 |
|--------|------|
| 30001 | 不能重复点赞 |
| 30002 | 尚未点赞 |
| 30003 | 评论不存在 |
| 30004 | 评论已被删除 |
| 30005 | 不能收藏自己的内容 |

| 业务码 | 说明 |
|--------|------|
| 40001 | 文件格式不支持 |
| 40002 | 文件大小超限 |
| 40003 | 上传文件为空 |
| 40004 | 上传失败，请重试 |

| 业务码 | 说明 |
|--------|------|
| 50001 | 发送消息频率过快 |
| 50002 | 不能给自己发私信 |
| 50003 | 对方已屏蔽你 |

### 8.3 错误响应示例

```json
{
    "code": 401,
    "message": "Token已过期，请重新登录",
    "data": null,
    "timestamp": 1718000000000,
    "requestId": "req-uuid-123"
}
```

```json
{
    "code": 429,
    "message": "请求过于频繁，请稍后再试",
    "data": {
        "retryAfter": 60
    },
    "timestamp": 1718000000000,
    "requestId": "req-uuid-456"
}
```

---

**文档版本**: v1.0
**最后更新**: 2026-06-11
**维护人**: Langtou DevOps Team
