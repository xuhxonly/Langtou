# 榔头(Langtou)社交内容社区 - Redis 缓存设计文档

## 一、设计原则

1. **缓存分层**：L1 本地缓存（Caffeine）+ L2 分布式缓存（Redis）
2. **缓存穿透防护**：布隆过滤器 + 空值缓存
3. **缓存雪崩防护**：随机过期时间 + 热点Key永不过期 + 异步续期
4. **缓存一致性**：Cache-Aside 模式，更新时先更新数据库再删除缓存
5. **大Key拆分**：Hash 分桶存储，避免单个 Key 过大

---

## 二、Key 命名规范

```
lt:{module}:{sub_module}:{identifier}
```

| 组成部分 | 说明 |
|---------|------|
| `lt` | 项目前缀（Langtou） |
| `module` | 业务模块（user/note/feed/social等） |
| `sub_module` | 子模块（profile/counter/session等） |
| `identifier` | 唯一标识（用户ID/笔记ID等） |

---

## 三、用户会话缓存

### 3.1 用户登录 Token
```
Key:     lt:user:session:{token}
Value:   Hash { user_id, username, device_type, login_time }
TTL:     7天（refresh_token 30天）
示例:    lt:user:session:a1b2c3d4e5f6
```

### 3.2 用户信息缓存
```
Key:     lt:user:profile:{user_id}
Value:   Hash { id, username, nickname, avatar_url, bio, follower_count, following_count, note_count }
TTL:     1小时 + 随机300秒
示例:    lt:user:profile:10086
```

### 3.3 用户设置缓存
```
Key:     lt:user:settings:{user_id}
Value:   Hash { privacy_level, notify_enabled, lang, theme }
TTL:     24小时
示例:    lt:user:settings:10086
```

---

## 四、Feed 流缓存

### 4.1 用户关注 Feed 流（时间线）
```
Key:     lt:feed:timeline:{user_id}
Value:   SortedSet { score: timestamp, member: note_id }
TTL:     7天（冷数据自动淘汰）
示例:    lt:feed:timeline:10086
说明:    存储用户关注的人的笔记ID，按时间倒序
容量:    保留最近 1000 条
```

### 4.2 推荐 Feed 流
```
Key:     lt:feed:recommend:{user_id}
Value:   List [ note_id, note_id, ... ]
TTL:     30分钟
示例:    lt:feed:recommend:10086
说明:    算法生成的推荐内容，定期刷新
```

### 4.3 热门 Feed 流（全局）
```
Key:     lt:feed:hot:{category}
Value:   SortedSet { score: hot_score, member: note_id }
TTL:     1小时
示例:    lt:feed:hot:all
          lt:feed:hot:food
          lt:feed:hot:travel
说明:    按热度分品类缓存，每小时更新
```

---

## 五、笔记内容缓存

### 5.1 笔记详情缓存
```
Key:     lt:note:detail:{note_id}
Value:   Hash { id, user_id, title, content, images, tags, location, like_count, comment_count, ... }
TTL:     2小时 + 随机600秒
示例:    lt:note:detail:9527
```

### 5.2 笔记点赞用户集合（用于判断用户是否已点赞）
```
Key:     lt:note:liked_users:{note_id}
Value:   Set { user_id, user_id, ... }
TTL:     24小时
示例:    lt:note:liked_users:9527
说明:    存储点赞用户ID集合，用于快速判断用户是否点赞
容量:    保留最近 5000 个用户ID
```

### 5.3 笔记评论列表缓存
```
Key:     lt:note:comments:{note_id}
Value:   SortedSet { score: created_at, member: comment_json }
TTL:     30分钟
示例:    lt:note:comments:9527
说明:    热门笔记的评论缓存，冷笔记不缓存
```

---

## 六、社交关系缓存

### 6.1 用户关注列表
```
Key:     lt:social:following:{user_id}
Value:   Set { following_id, following_id, ... }
TTL:     24小时
示例:    lt:social:following:10086
说明:    用户关注的人ID集合
```

### 6.2 用户粉丝列表
```
Key:     lt:social:followers:{user_id}
Value:   Set { follower_id, follower_id, ... }
TTL:     24小时
示例:    lt:social:followers:10086
说明:    用户的粉丝ID集合
```

### 6.3 互相关注好友列表
```
Key:     lt:social:friends:{user_id}
Value:   Set { friend_id, friend_id, ... }
TTL:     24小时
示例:    lt:social:friends:10086
说明:    通过 SINTER 计算 following 和 followers 的交集
```

### 6.4 用户是否关注某人的快速判断
```
Key:     lt:social:is_following:{follower_id}:{following_id}
Value:   String "1"
TTL:     24小时
示例:    lt:social:is_following:10086:10010
说明:    用于快速判断关注关系，避免查询数据库
```

---

## 七、计数器缓存

### 7.1 用户计数器（Hash 分桶）
```
Key:     lt:counter:user:{user_id}
Value:   Hash { follower_count, following_count, note_count, like_count, unread_notify }
TTL:     永不过期（异步续期）
示例:    lt:counter:user:10086
说明:    用户相关计数，定期同步到MySQL
```

### 7.2 笔记计数器
```
Key:     lt:counter:note:{note_id}
Value:   Hash { like_count, comment_count, collect_count, share_count, view_count }
TTL:     永不过期（异步续期）
示例:    lt:counter:note:9527
说明:    笔记互动计数，使用 Redis HINCRBY 原子递增
```

### 7.3 全局计数器（日活、总用户数等）
```
Key:     lt:counter:global:dau:{yyyyMMdd}
Value:   HyperLogLog { user_id }
TTL:     30天
示例:    lt:counter:global:dau:20240610
说明:    日活统计，使用 HyperLogLog 节省内存

Key:     lt:counter:global:total_users
Value:   String "1250000"
TTL:     5分钟
示例:    lt:counter:global:total_users
```

---

## 八、用户画像缓存

### 8.1 用户兴趣标签
```
Key:     lt:profile:interests:{user_id}
Value:   SortedSet { score: weight, member: tag_name }
TTL:     7天
示例:    lt:profile:interests:10086
说明:    用户感兴趣的标签及权重，用于推荐算法
```

### 8.2 用户行为统计（最近7天）
```
Key:     lt:profile:behavior:{user_id}:{behavior_type}
Value:   SortedSet { score: timestamp, member: target_id }
TTL:     7天
示例:    lt:profile:behavior:10086:like
          lt:profile:behavior:10086:view
          lt:profile:behavior:10086:collect
说明:    记录用户最近的行为，用于推荐和画像分析
```

---

## 九、热点数据缓存

### 9.1 热点笔记 Top N
```
Key:     lt:hot:notes:{category}
Value:   SortedSet { score: hot_score, member: note_id }
TTL:     10分钟
示例:    lt:hot:notes:all
说明:    全站热门笔记，每10分钟计算一次
```

### 9.2 热搜关键词
```
Key:     lt:hot:search_keywords
Value:   SortedSet { score: search_count, member: keyword }
TTL:     1小时
示例:    lt:hot:search_keywords
说明:    热搜排行榜，每小时更新
```

### 9.3 热点用户
```
Key:     lt:hot:users
Value:   SortedSet { score: follower_growth, member: user_id }
TTL:     1小时
示例:    lt:hot:users
说明:    近期涨粉最快的用户
```

---

## 十、消息与通知缓存

### 10.1 用户未读消息数
```
Key:     lt:message:unread:{user_id}
Value:   Hash { total: 5, from_user_10086: 2, from_user_10010: 3 }
TTL:     永不过期
示例:    lt:message:unread:10086
说明:    未读消息计数，新消息时递增，读取时递减
```

### 10.2 用户未读通知数
```
Key:     lt:notify:unread:{user_id}
Value:   Hash { total: 10, like: 3, comment: 4, follow: 2, system: 1 }
TTL:     永不过期
示例:    lt:notify:unread:10086
说明:    未读通知分类计数
```

### 10.3 最近消息列表（会话列表）
```
Key:     lt:message:recent:{user_id}
Value:   SortedSet { score: last_msg_time, member: peer_user_id }
TTL:     7天
示例:    lt:message:recent:10086
说明:    用户最近联系过的人列表，用于会话列表展示
```

---

## 十一、限流与防刷缓存

### 11.1 接口限流（滑动窗口）
```
Key:     lt:ratelimit:{api_name}:{user_id}
Value:   SortedSet { score: timestamp, member: request_id }
TTL:     1分钟
示例:    lt:ratelimit:publish:10086
说明:    记录用户最近请求时间，用于限流判断
```

### 11.2 点赞防刷
```
Key:     lt:antispam:like:{user_id}:{note_id}
Value:   String "1"
TTL:     5秒
示例:    lt:antispam:like:10086:9527
说明:    防止用户快速重复点赞/取消点赞
```

### 11.3 发布频率限制
```
Key:     lt:ratelimit:publish:{user_id}
Value:   String "count"
TTL:     1小时
示例:    lt:ratelimit:publish:10086
说明:    限制用户每小时发布笔记数量
```

---

## 十二、分布式锁

### 12.1 业务分布式锁
```
Key:     lt:lock:{resource}:{id}
Value:   String "request_id"
TTL:     30秒（看门狗续期）
示例:    lt:lock:note:like:9527
          lt:lock:user:follow:10086:10010
说明:    使用 Redisson 实现可重入分布式锁
```

---

## 十三、缓存更新策略

| 场景 | 策略 | 说明 |
|-----|------|------|
| 用户信息更新 | 先更新DB，再删除缓存 | Cache-Aside |
| 笔记发布 | 写入DB + 写入自己时间线 + 异步推送给粉丝 | Write-Through + 异步 |
| 点赞/取消点赞 | 先更新Redis计数器，异步同步DB | 最终一致性 |
| 关注/取消关注 | 先更新DB，再删除关系缓存 + 更新计数器 | Cache-Aside |
| Feed流 | 定时任务刷新 + 实时推送结合 | 推拉结合 |

---

## 十四、Redis 集群配置建议

```
# 集群模式：Redis Cluster（6主6从）
# 分片策略：CRC16(key) % 16384

# 热点Key处理：
# 1. 本地缓存兜底（Caffeine）
# 2. Hash Tag 确保相关Key在同一分片（如 lt:counter:note:{note_id}）
# 3. 读写分离，从节点承载读请求

# 大Key监控：
# 1. 用户粉丝列表超过10万时拆分为多个分片Set
# 2. Feed流限制保留1000条，超出部分归档到冷存储
# 3. 定期扫描大Key，及时优化
```

---

## 十五、Key 汇总表

| Key 模式 | 类型 | TTL | 用途 |
|---------|------|-----|------|
| `lt:user:session:{token}` | Hash | 7天 | 用户登录会话 |
| `lt:user:profile:{user_id}` | Hash | 1小时 | 用户基本信息 |
| `lt:feed:timeline:{user_id}` | ZSet | 7天 | 用户关注Feed流 |
| `lt:feed:recommend:{user_id}` | List | 30分钟 | 推荐Feed流 |
| `lt:feed:hot:{category}` | ZSet | 1小时 | 热门内容 |
| `lt:note:detail:{note_id}` | Hash | 2小时 | 笔记详情 |
| `lt:note:liked_users:{note_id}` | Set | 24小时 | 笔记点赞用户 |
| `lt:social:following:{user_id}` | Set | 24小时 | 关注列表 |
| `lt:social:followers:{user_id}` | Set | 24小时 | 粉丝列表 |
| `lt:counter:user:{user_id}` | Hash | 永久 | 用户计数器 |
| `lt:counter:note:{note_id}` | Hash | 永久 | 笔记计数器 |
| `lt:profile:interests:{user_id}` | ZSet | 7天 | 用户兴趣画像 |
| `lt:hot:notes:{category}` | ZSet | 10分钟 | 热点笔记 |
| `lt:hot:search_keywords` | ZSet | 1小时 | 热搜关键词 |
| `lt:message:unread:{user_id}` | Hash | 永久 | 未读消息数 |
| `lt:notify:unread:{user_id}` | Hash | 永久 | 未读通知数 |
| `lt:ratelimit:{api}:{user_id}` | ZSet | 1分钟 | 限流窗口 |
| `lt:lock:{resource}:{id}` | String | 30秒 | 分布式锁 |
