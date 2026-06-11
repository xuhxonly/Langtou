# 榔头(Langtou)社交内容社区 - 数据库设计

## 项目概述

榔头(Langtou)是一个类似小红书的社交内容社区APP，采用 **MySQL + Redis + Elasticsearch + Kafka** 的混合存储方案。本文档描述了完整的数据库设计方案。

## 技术栈

| 组件 | 版本 | 用途 |
|-----|------|------|
| MySQL | 8.x | 主存储，用户、笔记、关系等核心数据 |
| Redis | 7.x | 缓存、计数器、Feed流、会话 |
| Elasticsearch | 8.x | 笔记全文搜索 |
| Kafka | 3.x | 异步消息、数据同步 |
| Flyway | 10.x | 数据库迁移工具 |

## 项目结构

```
langtou-database/
├── schema.sql                  # 完整数据库结构脚本（可直接执行）
├── data.sql                    # 测试数据初始化脚本
├── redis-design.md             # Redis 缓存设计文档
├── es-mapping.json             # Elasticsearch 索引 Mapping
├── README.md                   # 本文件
├── flyway/
│   ├── flyway.conf             # Flyway 配置文件
│   └── migrations/
│       ├── V1__init_schema.sql # 迁移：初始化表结构
│       └── V2__init_data.sql   # 迁移：初始化测试数据
└── docs/                       # 补充文档目录
```

## 快速开始

### 1. 直接使用 SQL 脚本

```bash
# 登录 MySQL
mysql -u root -p

# 执行建表脚本
source /workspace/langtou-database/schema.sql;

# 执行测试数据
source /workspace/langtou-database/data.sql;
```

### 2. 使用 Flyway 迁移

```bash
# 安装 Flyway CLI（如未安装）
# https://documentation.red-gate.com/fd/command-line-184127404.html

# 修改配置
vim flyway/flyway.conf
# 修改 flyway.user 和 flyway.password

# 执行迁移
flyway -configFiles=flyway/flyway.conf migrate

# 查看迁移状态
flyway -configFiles=flyway/flyway.conf info
```

### 3. 创建 Elasticsearch 索引

```bash
# 创建索引
curl -X PUT "localhost:9200/notes" \
  -H "Content-Type: application/json" \
  -d @es-mapping.json

# 验证索引
curl -X GET "localhost:9200/notes/_mapping"
```

## 数据库表结构

### 核心表概览

| 表名 | 说明 | 预估数据量 | 分表策略 |
|-----|------|-----------|---------|
| `users` | 用户表 | 亿级 | user_id % 128 |
| `notes` | 笔记表 | 十亿级 | user_id % 128 |
| `follows` | 关注关系表 | 百亿级 | follower_id % 128 |
| `likes` | 点赞表 | 千亿级 | user_id % 128 |
| `comments` | 评论表 | 百亿级 | note_id % 128 |
| `collections` | 收藏表 | 百亿级 | user_id % 128 |
| `tags` | 标签表 | 万级 | 全局表，不分片 |
| `note_tags` | 笔记标签关联表 | 十亿级 | note_id % 128 |
| `messages` | 消息表 | 百亿级 | sender_id % 128 |
| `notifications` | 通知表 | 百亿级 | user_id % 128 |

### 表设计详解

#### 1. users（用户表）

```sql
CREATE TABLE `users` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `username`        VARCHAR(32)  NOT NULL COMMENT '用户名',
    `nickname`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar_url`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
    `phone`           VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `email`           VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    `password_hash`   VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `bio`             VARCHAR(500) NOT NULL DEFAULT '' COMMENT '个人简介',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别',
    `birthday`        DATE         NULL COMMENT '生日',
    `location`        VARCHAR(128) NOT NULL DEFAULT '' COMMENT '所在地',
    `follower_count`  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '粉丝数',
    `following_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注数',
    `note_count`      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '笔记数',
    `like_count`      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '获赞数',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态',
    `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);
```

**索引设计：**
- `uk_username` - 用户名唯一索引
- `uk_phone` - 手机号唯一索引
- `uk_email` - 邮箱唯一索引
- `idx_status_created` - 按状态筛选用户
- `idx_nickname` - 昵称搜索

#### 2. notes（笔记表）

```sql
CREATE TABLE `notes` (
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    `title`         VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '标题',
    `content`       TEXT            NOT NULL COMMENT '正文内容',
    `images`        JSON            NOT NULL COMMENT '图片URL数组',
    `video_url`     VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '视频URL',
    `tags`          JSON            NOT NULL COMMENT '标签名称数组',
    `location`      VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '发布地点',
    `like_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评论数',
    `collect_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '收藏数',
    `share_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分享数',
    `view_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '浏览数',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '状态',
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);
```

**索引设计：**
- `idx_user_id_created` - 查询用户发布的笔记列表
- `idx_status_created` - Feed流按时间倒序查询
- `idx_location` - 按地点筛选笔记
- `idx_title` - 标题搜索（辅助索引）

#### 3. follows（关注关系表）

```sql
CREATE TABLE `follows` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `follower_id`  BIGINT UNSIGNED NOT NULL COMMENT '粉丝ID',
    `following_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
```

**索引设计：**
- `uk_follower_following` - 唯一索引，防止重复关注
- `idx_following_id` - 查询用户的粉丝列表

#### 4. likes（点赞表）

```sql
CREATE TABLE `likes` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `target_type` VARCHAR(16)     NOT NULL COMMENT '目标类型：note/comment',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
```

**索引设计：**
- `uk_user_target` - 唯一索引，防止重复点赞
- `idx_target` - 查询某笔记/评论的点赞用户
- `idx_created_at` - 按时间排序

#### 5. comments（评论表）

```sql
CREATE TABLE `comments` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
    `note_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父评论ID',
    `content`    TEXT            NOT NULL COMMENT '评论内容',
    `like_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '状态',
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);
```

**索引设计：**
- `idx_note_id_parent` - 查询笔记的评论列表（支持一级回复）
- `idx_user_id` - 查询用户的所有评论
- `idx_parent_id` - 查询某条评论的回复

### 其他表

详见 `schema.sql` 文件，包含完整的表结构和索引定义。

## 分库分表设计

### 分片策略

| 表名 | 分片键 | 分片算法 | 分片数 |
|-----|--------|---------|--------|
| users | id | id % 128 | 128 |
| notes | user_id | user_id % 128 | 128 |
| follows | follower_id | follower_id % 128 | 128 |
| likes | user_id | user_id % 128 | 128 |
| comments | note_id | note_id % 128 | 128 |
| collections | user_id | user_id % 128 | 128 |
| note_tags | note_id | note_id % 128 | 128 |
| messages | sender_id | sender_id % 128 | 128 |
| notifications | user_id | user_id % 128 | 128 |

### 全局表

| 表名 | 说明 |
|-----|------|
| tags | 标签表，数据量小，每个分片存储完整数据 |

### 跨分片查询处理

1. **用户关注列表**：按 follower_id 分片，单分片查询
2. **用户粉丝列表**：使用 `idx_following_id` 索引，需要聚合多个分片结果
3. **笔记详情 + 作者信息**：笔记按 user_id 分片，用户信息需要广播或缓存
4. **Feed流**：采用推拉结合策略，写时扩散到粉丝的时间线

## Redis 缓存设计

详见 `redis-design.md`，主要包含以下模块：

- **用户会话**：Token 管理、用户信息缓存
- **Feed流**：关注时间线、推荐流、热门内容
- **笔记缓存**：详情、点赞用户、评论列表
- **社交关系**：关注列表、粉丝列表、互关好友
- **计数器**：用户计数、笔记计数、全局统计
- **用户画像**：兴趣标签、行为统计
- **限流防刷**：接口限流、操作防重

## Elasticsearch 搜索设计

详见 `es-mapping.json`，主要特性：

- **中文分词**：集成 IK 分词器（ik_max_word / ik_smart）
- **拼音搜索**：支持拼音首字母和全拼搜索
- **多字段搜索**：标题（boost 3.0）、内容（boost 2.0）、标签
- **自动补全**：Completion Suggester 实现搜索建议
- **排序支持**：按热度、时间、相关性排序

### 创建索引示例

```bash
curl -X PUT "localhost:9200/notes" -H "Content-Type: application/json" -d @es-mapping.json
```

### 搜索示例

```bash
# 按标题和内容搜索
curl -X POST "localhost:9200/notes/_search" -H "Content-Type: application/json" -d '{
  "query": {
    "multi_match": {
      "query": "健身减脂",
      "fields": ["title^3", "content^2", "tags"]
    }
  },
  "sort": [
    { "hot_score": "desc" },
    { "created_at": "desc" }
  ],
  "from": 0,
  "size": 20
}'
```

## 数据同步策略

### MySQL -> Elasticsearch

1. **Canal 监听**：监听 MySQL binlog，实时同步到 Kafka
2. **Consumer 消费**：从 Kafka 消费消息，写入 ES
3. **定时补偿**：定时任务扫描变更，补偿同步

### MySQL -> Redis

1. **Cache-Aside**：查询时先查缓存，未命中再查DB并写入缓存
2. **Write-Through**：更新时先更新DB，再删除/更新缓存
3. **定时预热**：定时任务将热点数据预热到缓存

## 性能优化建议

1. **索引优化**：
   - 所有外键查询字段建立索引
   - 联合索引遵循最左前缀原则
   - 定期使用 `EXPLAIN` 分析慢查询

2. **查询优化**：
   - 分页查询使用游标（cursor）替代 OFFSET
   - 大表查询限制返回字段
   - 避免在索引列上使用函数

3. **缓存优化**：
   - 热点数据设置永不过期 + 异步续期
   - 缓存空值防止穿透
   - 大Key拆分，避免单个Key过大

4. **写入优化**：
   - 批量写入替代单条写入
   - 异步处理非核心数据（计数、通知）
   - 使用消息队列削峰填谷

## 安全规范

1. **密码存储**：使用 bcrypt 哈希，cost factor >= 10
2. **敏感字段**：手机号、邮箱加密存储
3. **SQL注入**：使用预编译语句，禁止字符串拼接
4. **数据脱敏**：日志中敏感信息脱敏处理

## 维护操作

```sql
-- 查看表状态
SHOW TABLE STATUS FROM langtou;

-- 查看索引使用情况
SELECT * FROM performance_schema.table_io_waits_summary_by_index_usage 
WHERE OBJECT_SCHEMA = 'langtou';

-- 查看慢查询
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;

-- 分析表
ANALYZE TABLE users, notes, follows, likes, comments;

-- 优化表
OPTIMIZE TABLE users, notes;
```

## 版本历史

| 版本 | 日期 | 说明 |
|-----|------|------|
| V1 | 2024-06-10 | 初始版本，包含10张核心表 |

## 贡献指南

1. 修改表结构时，请创建新的 Flyway 迁移脚本
2. 命名规范：`V{版本号}__{描述}.sql`
3. 所有脚本必须可重复执行（使用 `IF NOT EXISTS`）
4. 提交前在测试环境验证

## 联系方式

如有问题，请联系数据库团队。
