# 榔头（Langtou）内容社区平台 - 接口测试用例

## 文档信息

- 版本：v1.0
- 编制日期：2026-06-12
- 适用范围：User / Content / Interact / Message Service
- 测试工具：REST Assured / Postman / JMeter

---

## 一、用户模块（User Service：8081）

### 1.1 用户注册

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-REG-001 | /api/v1/user/register | POST | 手机号未注册 | phone=13800138000, password=Test@1234, verifyCode=123456 | 返回 code=200，data 包含 userId 和 token，数据库新增用户记录 | P0 |
| USER-REG-002 | /api/v1/user/register | POST | 手机号已注册 | phone=13800138000, password=Test@1234, verifyCode=123456 | 返回 code=10001，msg="手机号已注册"，数据库无重复记录 | P0 |
| USER-REG-003 | /api/v1/user/register | POST | - | phone=13800138000, password=123, verifyCode=123456 | 返回 code=400，msg="密码长度需 8-20 位且包含字母和数字" | P1 |
| USER-REG-004 | /api/v1/user/register | POST | - | phone=1380013800a, password=Test@1234, verifyCode=123456 | 返回 code=400，msg="手机号格式不正确" | P1 |
| USER-REG-005 | /api/v1/user/register | POST | - | phone=13800138000, password=Test@1234, verifyCode=999999 | 返回 code=10002，msg="验证码错误或已过期" | P1 |
| USER-REG-006 | /api/v1/user/register | POST | - | phone=空, password=空, verifyCode=空 | 返回 code=400，msg="参数校验失败" | P1 |

### 1.2 用户登录

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-LOGIN-001 | /api/v1/user/login | POST | 用户已注册且密码正确 | phone=13800138000, password=Test@1234 | 返回 code=200，data 包含 token、refreshToken、过期时间 | P0 |
| USER-LOGIN-002 | /api/v1/user/login | POST | 用户已注册 | phone=13800138000, password=WrongPass1 | 返回 code=10003，msg="账号或密码错误" | P0 |
| USER-LOGIN-003 | /api/v1/user/login | POST | - | phone=13800138000, password=Test@1234 | 连续失败 5 次后返回 code=10004，msg="账号已锁定，请 30 分钟后重试" | P1 |
| USER-LOGIN-004 | /api/v1/user/login | POST | - | phone=未注册手机号, password=Test@1234 | 返回 code=10003，msg="账号或密码错误" | P1 |
| USER-LOGIN-005 | /api/v1/user/login | POST | - | phone=13800138000, password=空 | 返回 code=400，msg="密码不能为空" | P1 |
| USER-LOGIN-006 | /api/v1/user/login | POST | 用户状态为禁用 | phone=禁用账号, password=Test@1234 | 返回 code=10005，msg="账号已被禁用" | P1 |

### 1.3 获取用户资料

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-PROFILE-001 | /api/v1/user/profile/{userId} | GET | 用户存在且已登录 | userId=10001, Header: Authorization=Bearer xxx | 返回 code=200，data 包含 nickname、avatar、bio、followCount、fanCount 等 | P0 |
| USER-PROFILE-002 | /api/v1/user/profile/{userId} | GET | 未登录 | userId=10001 | 返回 code=401，msg="请先登录" | P0 |
| USER-PROFILE-003 | /api/v1/user/profile/{userId} | GET | 已登录 | userId=99999（不存在） | 返回 code=10006，msg="用户不存在" | P1 |
| USER-PROFILE-004 | /api/v1/user/profile/{userId} | GET | 已登录 | userId=-1 | 返回 code=400，msg="用户ID格式错误" | P1 |
| USER-PROFILE-005 | /api/v1/user/profile/{userId} | GET | 已登录 | userId=10001，token 已过期 | 返回 code=401，msg="token 已过期" | P1 |

### 1.4 更新用户资料

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-UPDATE-001 | /api/v1/user/profile | PUT | 已登录 | nickname="新昵称", bio="新简介", gender=1 | 返回 code=200，数据库字段更新成功 | P0 |
| USER-UPDATE-002 | /api/v1/user/profile | PUT | 已登录 | nickname="超长昵称超过二十个字符长度限制测试" | 返回 code=400，msg="昵称长度不能超过 20 个字符" | P1 |
| USER-UPDATE-003 | /api/v1/user/profile | PUT | 已登录 | avatar=file（超过 5MB 的图片） | 返回 code=400，msg="头像大小不能超过 5MB" | P1 |
| USER-UPDATE-004 | /api/v1/user/profile | PUT | 未登录 | nickname="新昵称" | 返回 code=401，msg="请先登录" | P0 |
| USER-UPDATE-005 | /api/v1/user/profile | PUT | 已登录 | nickname="  "（纯空格） | 返回 code=400，msg="昵称不能为空" | P1 |
| USER-UPDATE-006 | /api/v1/user/profile | PUT | 已登录 | bio="正常简介", gender=3 | 返回 code=400，msg="性别参数非法" | P1 |

### 1.5 关注用户

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-FOLLOW-001 | /api/v1/user/follow | POST | 已登录，未关注该用户 | targetUserId=10002 | 返回 code=200，关注成功，followCount+1，对方 fanCount+1 | P0 |
| USER-FOLLOW-002 | /api/v1/user/follow | POST | 已登录，已关注该用户 | targetUserId=10002 | 返回 code=10007，msg="已关注该用户" | P1 |
| USER-FOLLOW-003 | /api/v1/user/follow | POST | 已登录 | targetUserId=10001（自己） | 返回 code=10008，msg="不能关注自己" | P1 |
| USER-FOLLOW-004 | /api/v1/user/follow | POST | 未登录 | targetUserId=10002 | 返回 code=401，msg="请先登录" | P0 |
| USER-FOLLOW-005 | /api/v1/user/follow | POST | 已登录 | targetUserId=99999（不存在） | 返回 code=10006，msg="用户不存在" | P1 |

### 1.6 取消关注

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| USER-UNFOLLOW-001 | /api/v1/user/unfollow | POST | 已登录，已关注该用户 | targetUserId=10002 | 返回 code=200，取消关注成功，followCount-1 | P0 |
| USER-UNFOLLOW-002 | /api/v1/user/unfollow | POST | 已登录，未关注该用户 | targetUserId=10002 | 返回 code=10009，msg="未关注该用户" | P1 |
| USER-UNFOLLOW-003 | /api/v1/user/unfollow | POST | 未登录 | targetUserId=10002 | 返回 code=401，msg="请先登录" | P0 |
| USER-UNFOLLOW-004 | /api/v1/user/unfollow | POST | 已登录 | targetUserId=99999 | 返回 code=10006，msg="用户不存在" | P1 |
| USER-UNFOLLOW-005 | /api/v1/user/unfollow | POST | 已登录 | targetUserId=空 | 返回 code=400，msg="参数不能为空" | P1 |

---

## 二、内容模块（Content Service：8082）

### 2.1 发布笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| CONTENT-PUB-001 | /api/v1/content/note | POST | 已登录 | title="测试笔记", content="内容", images=["url1","url2"], type=1 | 返回 code=200，data 包含 noteId，数据库新增笔记 | P0 |
| CONTENT-PUB-002 | /api/v1/content/note | POST | 已登录 | title="测试视频笔记", content="视频内容", videoUrl="url", type=2, duration=30 | 返回 code=200，data 包含 noteId，type=2 | P0 |
| CONTENT-PUB-003 | /api/v1/content/note | POST | 已登录 | title="", content="内容" | 返回 code=400，msg="标题不能为空" | P1 |
| CONTENT-PUB-004 | /api/v1/content/note | POST | 已登录 | title="测试", content="内容", images=[] | 返回 code=400，msg="图文笔记至少包含一张图片" | P1 |
| CONTENT-PUB-005 | /api/v1/content/note | POST | 已登录 | title="超长标题超过一百个字符长度限制的测试用例数据需要验证边界情况处理逻辑", content="内容" | 返回 code=400，msg="标题长度不能超过 100 个字符" | P1 |
| CONTENT-PUB-006 | /api/v1/content/note | POST | 未登录 | title="测试", content="内容" | 返回 code=401，msg="请先登录" | P0 |
| CONTENT-PUB-007 | /api/v1/content/note | POST | 已登录 | title="敏感词测试", content="内容包含违禁词" | 返回 code=10020，msg="内容包含敏感信息" | P1 |

### 2.2 获取笔记列表

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| CONTENT-LIST-001 | /api/v1/content/notes | GET | - | page=1, size=10, type=recommend | 返回 code=200，data.list 为笔记列表，total >= 0 | P0 |
| CONTENT-LIST-002 | /api/v1/content/notes | GET | 已登录 | page=1, size=10, type=follow | 返回 code=200，data.list 仅包含关注用户的笔记 | P0 |
| CONTENT-LIST-003 | /api/v1/content/notes | GET | - | page=0, size=10 | 返回 code=400，msg="页码必须大于等于 1" | P1 |
| CONTENT-LIST-004 | /api/v1/content/notes | GET | - | page=1, size=101 | 返回 code=400，msg="每页数量不能超过 100" | P1 |
| CONTENT-LIST-005 | /api/v1/content/notes | GET | - | page=1, size=10, type=nearby, lat=39.9, lng=116.4 | 返回 code=200，data.list 按距离排序 | P1 |
| CONTENT-LIST-006 | /api/v1/content/notes | GET | - | page=9999, size=10 | 返回 code=200，data.list 为空 | P1 |

### 2.3 搜索笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| CONTENT-SEARCH-001 | /api/v1/content/search | GET | ES 索引正常 | keyword="美食", page=1, size=10 | 返回 code=200，data.list 包含标题/内容匹配"美食"的笔记 | P0 |
| CONTENT-SEARCH-002 | /api/v1/content/search | GET | - | keyword="", page=1, size=10 | 返回 code=400，msg="搜索关键词不能为空" | P1 |
| CONTENT-SEARCH-003 | /api/v1/content/search | GET | - | keyword="不存在的关键词xyz", page=1, size=10 | 返回 code=200，data.list 为空，total=0 | P1 |
| CONTENT-SEARCH-004 | /api/v1/content/search | GET | - | keyword="<script>alert(1)</script>", page=1, size=10 | 返回 code=200，XSS 标签被转义，无脚本执行 | P1 |
| CONTENT-SEARCH-005 | /api/v1/content/search | GET | - | keyword="美食", page=1, size=10, sort=latest | 返回 code=200，data.list 按时间倒序排列 | P1 |
| CONTENT-SEARCH-006 | /api/v1/content/search | GET | - | keyword="美", page=1, size=10 | 返回 code=400，msg="关键词长度不能少于 2 个字符" | P1 |

### 2.4 获取笔记详情

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| CONTENT-DETAIL-001 | /api/v1/content/note/{noteId} | GET | 笔记存在 | noteId=20001 | 返回 code=200，data 包含完整笔记信息、作者信息、点赞/评论/收藏数 | P0 |
| CONTENT-DETAIL-002 | /api/v1/content/note/{noteId} | GET | 笔记存在，已登录 | noteId=20001, Header: Authorization=Bearer xxx | 返回 code=200，data 额外包含 isLiked、isCollected、isFollowed 状态 | P0 |
| CONTENT-DETAIL-003 | /api/v1/content/note/{noteId} | GET | - | noteId=99999（不存在） | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| CONTENT-DETAIL-004 | /api/v1/content/note/{noteId} | GET | - | noteId=-1 | 返回 code=400，msg="笔记ID格式错误" | P1 |
| CONTENT-DETAIL-005 | /api/v1/content/note/{noteId} | GET | 笔记存在但已删除 | noteId=20002（已删除） | 返回 code=10021，msg="笔记不存在或已删除" | P1 |

### 2.5 删除笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| CONTENT-DEL-001 | /api/v1/content/note/{noteId} | DELETE | 已登录，笔记作者为当前用户 | noteId=20001 | 返回 code=200，笔记状态变更为已删除，ES 索引同步删除 | P0 |
| CONTENT-DEL-002 | /api/v1/content/note/{noteId} | DELETE | 已登录，笔记作者非当前用户 | noteId=20003 | 返回 code=403，msg="无权删除他人笔记" | P0 |
| CONTENT-DEL-003 | /api/v1/content/note/{noteId} | DELETE | 未登录 | noteId=20001 | 返回 code=401，msg="请先登录" | P0 |
| CONTENT-DEL-004 | /api/v1/content/note/{noteId} | DELETE | 已登录 | noteId=99999 | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| CONTENT-DEL-005 | /api/v1/content/note/{noteId} | DELETE | 已登录，管理员身份 | noteId=20004（他人笔记） | 返回 code=200，管理员可删除违规笔记 | P1 |

---

## 三、互动模块（Interact Service：8083）

### 3.1 点赞笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-LIKE-001 | /api/v1/interact/like | POST | 已登录，未点赞 | noteId=20001 | 返回 code=200，点赞成功，笔记 likeCount+1，Redis 缓存更新 | P0 |
| INTERACT-LIKE-002 | /api/v1/interact/like | POST | 已登录，已点赞 | noteId=20001 | 返回 code=10030，msg="已点赞" | P1 |
| INTERACT-LIKE-003 | /api/v1/interact/like | POST | 未登录 | noteId=20001 | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-LIKE-004 | /api/v1/interact/like | POST | 已登录 | noteId=99999 | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| INTERACT-LIKE-005 | /api/v1/interact/like | POST | 已登录 | noteId=空 | 返回 code=400，msg="笔记ID不能为空" | P1 |
| INTERACT-LIKE-006 | /api/v1/interact/like | POST | 已登录 | noteId=20001（并发场景） | 幂等性验证，likeCount 只增加 1 | P1 |

### 3.2 取消点赞

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-UNLIKE-001 | /api/v1/interact/unlike | POST | 已登录，已点赞 | noteId=20001 | 返回 code=200，取消点赞成功，likeCount-1 | P0 |
| INTERACT-UNLIKE-002 | /api/v1/interact/unlike | POST | 已登录，未点赞 | noteId=20001 | 返回 code=10031，msg="未点赞" | P1 |
| INTERACT-UNLIKE-003 | /api/v1/interact/unlike | POST | 未登录 | noteId=20001 | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-UNLIKE-004 | /api/v1/interact/unlike | POST | 已登录 | noteId=99999 | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| INTERACT-UNLIKE-005 | /api/v1/interact/unlike | POST | 已登录 | noteId=20001（并发场景） | 幂等性验证，likeCount 只减少 1 | P1 |

### 3.3 评论笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-COMMENT-001 | /api/v1/interact/comment | POST | 已登录，笔记存在 | noteId=20001, content="这是一条评论" | 返回 code=200，data 包含 commentId，commentCount+1 | P0 |
| INTERACT-COMMENT-002 | /api/v1/interact/comment | POST | 已登录 | noteId=20001, content="" | 返回 code=400，msg="评论内容不能为空" | P1 |
| INTERACT-COMMENT-003 | /api/v1/interact/comment | POST | 已登录 | noteId=20001, content="超长评论超过五百个字符长度限制的测试数据需要验证边界情况处理逻辑...（>500字）" | 返回 code=400，msg="评论内容不能超过 500 个字符" | P1 |
| INTERACT-COMMENT-004 | /api/v1/interact/comment | POST | 未登录 | noteId=20001, content="评论" | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-COMMENT-005 | /api/v1/interact/comment | POST | 已登录 | noteId=99999, content="评论" | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| INTERACT-COMMENT-006 | /api/v1/interact/comment | POST | 已登录 | noteId=20001, content="敏感词评论" | 返回 code=10020，msg="内容包含敏感信息" | P1 |

### 3.4 回复评论

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-REPLY-001 | /api/v1/interact/comment/reply | POST | 已登录，评论存在 | commentId=30001, content="这是一条回复", replyToUserId=10002 | 返回 code=200，data 包含 replyId | P0 |
| INTERACT-REPLY-002 | /api/v1/interact/comment/reply | POST | 已登录 | commentId=30001, content="", replyToUserId=10002 | 返回 code=400，msg="回复内容不能为空" | P1 |
| INTERACT-REPLY-003 | /api/v1/interact/comment/reply | POST | 已登录 | commentId=99999, content="回复", replyToUserId=10002 | 返回 code=10032，msg="评论不存在" | P1 |
| INTERACT-REPLY-004 | /api/v1/interact/comment/reply | POST | 未登录 | commentId=30001, content="回复", replyToUserId=10002 | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-REPLY-005 | /api/v1/interact/comment/reply | POST | 已登录 | commentId=30001, content="回复", replyToUserId=99999 | 返回 code=10006，msg="回复目标用户不存在" | P1 |
| INTERACT-REPLY-006 | /api/v1/interact/comment/reply | POST | 已登录 | commentId=30001, content="@用户 回复内容", replyToUserId=10002 | 返回 code=200，支持 @ 提及功能 | P1 |

### 3.5 收藏笔记

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-COLLECT-001 | /api/v1/interact/collect | POST | 已登录，未收藏 | noteId=20001, folderId=0 | 返回 code=200，收藏成功，collectCount+1 | P0 |
| INTERACT-COLLECT-002 | /api/v1/interact/collect | POST | 已登录，未收藏 | noteId=20001, folderId=40001（指定收藏夹） | 返回 code=200，收藏到指定收藏夹 | P1 |
| INTERACT-COLLECT-003 | /api/v1/interact/collect | POST | 已登录，已收藏 | noteId=20001 | 返回 code=10033，msg="已收藏" | P1 |
| INTERACT-COLLECT-004 | /api/v1/interact/collect | POST | 未登录 | noteId=20001 | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-COLLECT-005 | /api/v1/interact/collect | POST | 已登录 | noteId=99999 | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| INTERACT-COLLECT-006 | /api/v1/interact/collect | POST | 已登录 | noteId=20001, folderId=99999 | 返回 code=10034，msg="收藏夹不存在" | P1 |

### 3.6 取消收藏

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| INTERACT-UNCOLLECT-001 | /api/v1/interact/uncollect | POST | 已登录，已收藏 | noteId=20001 | 返回 code=200，取消收藏成功，collectCount-1 | P0 |
| INTERACT-UNCOLLECT-002 | /api/v1/interact/uncollect | POST | 已登录，未收藏 | noteId=20001 | 返回 code=10035，msg="未收藏" | P1 |
| INTERACT-UNCOLLECT-003 | /api/v1/interact/uncollect | POST | 未登录 | noteId=20001 | 返回 code=401，msg="请先登录" | P0 |
| INTERACT-UNCOLLECT-004 | /api/v1/interact/uncollect | POST | 已登录 | noteId=99999 | 返回 code=10021，msg="笔记不存在或已删除" | P1 |
| INTERACT-UNCOLLECT-005 | /api/v1/interact/uncollect | POST | 已登录 | noteId=20001（并发场景） | 幂等性验证，collectCount 只减少 1 | P1 |

---

## 四、消息模块（Message Service：8084）

### 4.1 发送私信

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| MSG-SEND-001 | /api/v1/message/send | POST | 已登录，对方用户存在 | toUserId=10002, content="你好，私信测试" | 返回 code=200，data 包含 messageId，对方收到消息通知 | P0 |
| MSG-SEND-002 | /api/v1/message/send | POST | 已登录 | toUserId=10002, content="" | 返回 code=400，msg="消息内容不能为空" | P1 |
| MSG-SEND-003 | /api/v1/message/send | POST | 已登录 | toUserId=10002, content="超长消息超过两千个字符长度限制的测试数据...（>2000字）" | 返回 code=400，msg="消息内容不能超过 2000 个字符" | P1 |
| MSG-SEND-004 | /api/v1/message/send | POST | 已登录 | toUserId=10001（自己） | 返回 code=10040，msg="不能给自己发送私信" | P1 |
| MSG-SEND-005 | /api/v1/message/send | POST | 未登录 | toUserId=10002, content="测试" | 返回 code=401，msg="请先登录" | P0 |
| MSG-SEND-006 | /api/v1/message/send | POST | 已登录 | toUserId=99999 | 返回 code=10006，msg="用户不存在" | P1 |
| MSG-SEND-007 | /api/v1/message/send | POST | 已登录，被对方拉黑 | toUserId=10003（已拉黑） | 返回 code=10041，msg="对方拒收您的消息" | P1 |

### 4.2 获取会话列表

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| MSG-CONV-001 | /api/v1/message/conversations | GET | 已登录，有会话记录 | page=1, size=20 | 返回 code=200，data.list 按最后消息时间倒序排列 | P0 |
| MSG-CONV-002 | /api/v1/message/conversations | GET | 已登录，无会话记录 | page=1, size=20 | 返回 code=200，data.list 为空 | P1 |
| MSG-CONV-003 | /api/v1/message/conversations | GET | 未登录 | page=1, size=20 | 返回 code=401，msg="请先登录" | P0 |
| MSG-CONV-004 | /api/v1/message/conversations | GET | 已登录 | page=1, size=101 | 返回 code=400，msg="每页数量不能超过 100" | P1 |
| MSG-CONV-005 | /api/v1/message/conversations | GET | 已登录 | page=1, size=20 | 返回数据中包含 unreadCount、lastMessage、lastMessageTime | P1 |

### 4.3 获取消息列表

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| MSG-LIST-001 | /api/v1/message/list | GET | 已登录，有消息记录 | conversationId=50001, page=1, size=20 | 返回 code=200，data.list 为消息列表，按时间正序排列 | P0 |
| MSG-LIST-002 | /api/v1/message/list | GET | 已登录 | conversationId=50001, page=1, size=20 | 返回数据中包含 isSelf（是否自己发送）、sendTime、readStatus | P1 |
| MSG-LIST-003 | /api/v1/message/list | GET | 未登录 | conversationId=50001 | 返回 code=401，msg="请先登录" | P0 |
| MSG-LIST-004 | /api/v1/message/list | GET | 已登录 | conversationId=99999 | 返回 code=10042，msg="会话不存在" | P1 |
| MSG-LIST-005 | /api/v1/message/list | GET | 已登录 | conversationId=50001, page=1, size=20 | 分页验证，page=2 返回下一页数据无重复 | P1 |
| MSG-LIST-006 | /api/v1/message/list | GET | 已登录 | conversationId=50001, page=1, size=20 | 非当前用户会话返回 code=403，msg="无权查看该会话" | P0 |

### 4.4 标记消息已读

| 用例ID | 接口路径 | 请求方法 | 前置条件 | 请求参数 | 预期结果 | 优先级 |
|--------|----------|----------|----------|----------|----------|--------|
| MSG-READ-001 | /api/v1/message/read | POST | 已登录，有未读消息 | conversationId=50001 | 返回 code=200，该会话未读消息标记为已读，unreadCount 清零 | P0 |
| MSG-READ-002 | /api/v1/message/read | POST | 已登录，无未读消息 | conversationId=50001 | 返回 code=200，无变更 | P1 |
| MSG-READ-003 | /api/v1/message/read | POST | 未登录 | conversationId=50001 | 返回 code=401，msg="请先登录" | P0 |
| MSG-READ-004 | /api/v1/message/read | POST | 已登录 | conversationId=99999 | 返回 code=10042，msg="会话不存在" | P1 |
| MSG-READ-005 | /api/v1/message/read | POST | 已登录 | conversationId=50001（非自己会话） | 返回 code=403，msg="无权操作该会话" | P0 |
| MSG-READ-006 | /api/v1/message/read | POST | 已登录 | conversationId=50001, messageIds=[60001,60002] | 返回 code=200，指定消息标记为已读 | P1 |

---

## 五、通用规范

### 5.1 接口响应规范

所有接口统一返回格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": {},
  "timestamp": 1718188800000,
  "traceId": "abc123def456"
}
```

### 5.2 鉴权规范

- 登录/注册接口无需 Token
- 其他接口需在 Header 中携带：`Authorization: Bearer {token}`
- Token 过期返回 code=401，需使用 refreshToken 刷新

### 5.3 幂等性规范

- 点赞/收藏/关注等操作需保证幂等
- 关键写操作接口支持 `idempotency-key` 请求头

### 5.4 分页规范

- 默认 page=1, size=10
- 最大 size=100
- 返回字段包含：list, total, page, size, pages

---

*文档版本：v1.0*
*编制日期：2026-06-12*
*维护团队：榔头（Langtou）QA 团队*
