-- ============================================================
-- 榔头(Langtou)社交内容社区APP - 数据库设计
-- MySQL 8.x / InnoDB / utf8mb4
-- ============================================================
-- 设计原则：
-- 1. 统一使用 utf8mb4 字符集，支持 emoji 和中文
-- 2. 主键统一使用 BIGINT UNSIGNED AUTO_INCREMENT
-- 3. 时间戳使用 DATETIME(3) 支持毫秒级精度
-- 4. 软删除使用 status 字段（0-正常 1-删除 2-冻结）
-- 5. JSON 字段用于存储变长数组数据
-- 6. 预留分库分表扩展性（sharding_key 设计）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `langtou`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `langtou`;

-- ============================================================
-- 1. 用户表 (users)
-- 分表策略：按 user_id % 128 分表（预留）
-- ============================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username`        VARCHAR(32)  NOT NULL COMMENT '用户名（唯一标识）',
    `nickname`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar_url`      VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
    `phone`           VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `email`           VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    `password_hash`   VARCHAR(255) NOT NULL COMMENT '密码哈希（bcrypt）',
    `bio`             VARCHAR(500) NOT NULL DEFAULT '' COMMENT '个人简介',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `birthday`        DATE         NULL COMMENT '生日',
    `location`        VARCHAR(128) NOT NULL DEFAULT '' COMMENT '所在地',
    `follower_count`  INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '粉丝数',
    `following_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关注数',
    `note_count`      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '笔记数',
    `like_count`      INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '获赞数',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-注销 2-冻结',
    `created_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 笔记表 (notes)
-- 分表策略：按 user_id % 128 分表（用户维度）
-- 查询场景：按用户查笔记、按时间倒序Feed流、按标签搜索
-- ============================================================
DROP TABLE IF EXISTS `notes`;
CREATE TABLE `notes` (
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '笔记ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    `title`         VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '标题',
    `content`       TEXT            NOT NULL COMMENT '正文内容',
    `images`        JSON            NOT NULL COMMENT '图片URL数组',
    `video_url`     VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '视频URL',
    `tags`          JSON            NOT NULL COMMENT '标签名称数组（冗余存储加速查询）',
    `location`      VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '发布地点',
    `like_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '评论数',
    `collect_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '收藏数',
    `share_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '分享数',
    `view_count`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '浏览数',
    `status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-删除 2-审核中 3-仅自己可见',
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    KEY `idx_user_id_created` (`user_id`, `created_at`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_location` (`location`),
    KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- ============================================================
-- 3. 关注关系表 (follows)
-- 分表策略：按 follower_id % 128 分表（粉丝维度）
-- 查询场景：我的关注列表、我的粉丝列表、是否已关注
-- ============================================================
DROP TABLE IF EXISTS `follows`;
CREATE TABLE `follows` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    `follower_id`  BIGINT UNSIGNED NOT NULL COMMENT '粉丝ID（关注者）',
    `following_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',
    `created_at`   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关注时间',

    UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    KEY `idx_following_id` (`following_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系表';

-- ============================================================
-- 4. 点赞表 (likes)
-- 分表策略：按 user_id % 128 分表（用户维度）
-- 查询场景：我的点赞列表、某笔记/评论的点赞用户、是否已点赞
-- ============================================================
DROP TABLE IF EXISTS `likes`;
CREATE TABLE `likes` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标ID（笔记ID或评论ID）',
    `target_type` VARCHAR(16)     NOT NULL COMMENT '目标类型：note/comment',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '点赞时间',

    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    KEY `idx_target` (`target_id`, `target_type`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- ============================================================
-- 5. 评论表 (comments)
-- 分表策略：按 note_id % 128 分表（笔记维度）
-- 查询场景：某笔记的评论列表、我的评论、评论回复
-- ============================================================
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
    `note_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父评论ID（0表示顶级评论）',
    `content`    TEXT            NOT NULL COMMENT '评论内容',
    `like_count` INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-删除',
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    KEY `idx_note_id_parent` (`note_id`, `parent_id`, `created_at`),
    KEY `idx_user_id` (`user_id`, `created_at`),
    KEY `idx_parent_id` (`parent_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================================
-- 6. 收藏表 (collections)
-- 分表策略：按 user_id % 128 分表（用户维度）
-- 查询场景：我的收藏列表、某笔记被收藏次数
-- ============================================================
DROP TABLE IF EXISTS `collections`;
CREATE TABLE `collections` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `note_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',

    UNIQUE KEY `uk_user_note` (`user_id`, `note_id`),
    KEY `idx_note_id` (`note_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ============================================================
-- 7. 标签表 (tags)
-- 全局表，不分片
-- ============================================================
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    `name`       VARCHAR(64)  NOT NULL COMMENT '标签名称',
    `note_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联笔记数',
    `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-禁用',
    `created_at` DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_note_count` (`note_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ============================================================
-- 8. 笔记标签关联表 (note_tags)
-- 分表策略：与 notes 表一致，按 note_id % 128 分表
-- ============================================================
DROP TABLE IF EXISTS `note_tags`;
CREATE TABLE `note_tags` (
    `id`      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `note_id` BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `tag_id`  BIGINT UNSIGNED NOT NULL COMMENT '标签ID',

    UNIQUE KEY `uk_note_tag` (`note_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记标签关联表';

-- ============================================================
-- 9. 消息表 (messages)
-- 分表策略：按 sender_id % 128 分表（发送者维度）+ 冗余存储
-- 查询场景：聊天记录、未读消息数
-- ============================================================
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `sender_id`   BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `content`     TEXT            NOT NULL COMMENT '消息内容',
    `type`        VARCHAR(16)     NOT NULL DEFAULT 'text' COMMENT '类型：text/image/video',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-未读 1-已读 2-撤回',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    KEY `idx_sender_receiver` (`sender_id`, `receiver_id`, `created_at`),
    KEY `idx_receiver_status` (`receiver_id`, `status`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ============================================================
-- 10. 通知表 (notifications)
-- 分表策略：按 user_id % 128 分表（用户维度）
-- 查询场景：我的通知列表、未读通知数
-- ============================================================
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID',
    `type`        VARCHAR(32)     NOT NULL COMMENT '类型：like/follow/comment/system',
    `source_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '来源ID（触发通知的对象ID）',
    `source_type` VARCHAR(16)     NOT NULL DEFAULT '' COMMENT '来源类型：note/comment/user',
    `content`     VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '通知内容摘要',
    `is_read`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    KEY `idx_user_id_read` (`user_id`, `is_read`, `created_at`),
    KEY `idx_user_id_type` (`user_id`, `type`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

SET FOREIGN_KEY_CHECKS = 1;
