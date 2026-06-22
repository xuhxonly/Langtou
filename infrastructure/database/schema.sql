-- ============================================================
-- 榔头(Langtou)社交内容社区APP - 数据库设计
-- MySQL 8.x / InnoDB / utf8mb4
-- ============================================================
-- 设计原则：
-- 1. 单库设计：MVP阶段使用 langtou 单库，不分库
-- 2. 统一使用 utf8mb4 字符集，支持 emoji 和中文
-- 3. 主键统一使用 BIGINT UNSIGNED AUTO_INCREMENT
-- 4. 时间戳使用 DATETIME(3) 支持毫秒级精度，统一 xxx_at 命名
-- 5. 表名使用单数，下划线分隔
-- 6. 字段名统一使用下划线分隔
-- 7. 布尔字段统一 is_xxx 命名
-- 8. 外键字段统一 xxx_id 命名
-- 9. 索引名：idx_表名_字段名，唯一索引：uk_表名_字段名
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `langtou`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `langtou`;

-- ============================================================
-- 1. 用户表 (user)
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username`        VARCHAR(50)  NOT NULL COMMENT '用户名',
    `nickname`        VARCHAR(100) DEFAULT '' COMMENT '昵称',
    `avatar_url`      VARCHAR(500) DEFAULT '' COMMENT '头像URL',
    `phone`           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `password_hash`   VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `bio`             TEXT COMMENT '个人简介',
    `gender`          TINYINT DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    `birthday`        DATE DEFAULT NULL COMMENT '生日',
    `location`        VARCHAR(100) DEFAULT '' COMMENT '所在地',
    `follower_count`  INT UNSIGNED DEFAULT 0 COMMENT '粉丝数',
    `following_count` INT UNSIGNED DEFAULT 0 COMMENT '关注数',
    `note_count`      INT UNSIGNED DEFAULT 0 COMMENT '笔记数',
    `liked_count`     INT UNSIGNED DEFAULT 0 COMMENT '获赞数',
    `status`          TINYINT DEFAULT 1 COMMENT '状态: 0禁用 1正常',
    `created_at`      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 笔记表 (note)
-- ============================================================
DROP TABLE IF EXISTS `note`;
CREATE TABLE `note` (
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '笔记ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    `title`         VARCHAR(200) DEFAULT '' COMMENT '标题',
    `content`       TEXT COMMENT '正文内容',
    `images`        JSON DEFAULT NULL COMMENT '图片列表',
    `video_url`     VARCHAR(500) DEFAULT NULL COMMENT '视频URL',
    `cover_url`     VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    `location`      VARCHAR(100) DEFAULT '' COMMENT '地理位置',
    `like_count`    INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT UNSIGNED DEFAULT 0 COMMENT '评论数',
    `collect_count` INT UNSIGNED DEFAULT 0 COMMENT '收藏数',
    `share_count`   INT UNSIGNED DEFAULT 0 COMMENT '分享数',
    `view_count`    INT UNSIGNED DEFAULT 0 COMMENT '浏览数',
    `status`        TINYINT DEFAULT 1 COMMENT '状态: 0审核中 1正常 2下架 3删除',
    `created_at`    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发布时间',
    `updated_at`    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status_created` (`status`, `created_at`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

-- ============================================================
-- 3. 标签表 (tag)
-- ============================================================
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    `name`       VARCHAR(50) NOT NULL COMMENT '标签名',
    `icon`       VARCHAR(100) DEFAULT '' COMMENT '图标',
    `note_count` INT UNSIGNED DEFAULT 0 COMMENT '关联笔记数',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- ============================================================
-- 4. 笔记标签关联表 (note_tag)
-- ============================================================
DROP TABLE IF EXISTS `note_tag`;
CREATE TABLE `note_tag` (
    `id`      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `note_id` BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `tag_id`  BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    UNIQUE KEY `uk_note_tag` (`note_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记标签关联';

-- ============================================================
-- 5. 关注关系表 (follow)
-- ============================================================
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    `follower_id`  BIGINT UNSIGNED NOT NULL COMMENT '关注者',
    `following_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
    `created_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '关注时间',
    UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    KEY `idx_following_id` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注关系';

-- ============================================================
-- 6. 点赞记录表 (like_record)
-- ============================================================
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_id`   BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `target_type` TINYINT NOT NULL COMMENT '目标类型: 1笔记 2评论',
    `created_at`  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '点赞时间',
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    KEY `idx_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞记录';

-- ============================================================
-- 7. 评论表 (comment)
-- ============================================================
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '评论者ID',
    `note_id`      BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `parent_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID',
    `reply_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复目标用户',
    `content`      TEXT NOT NULL COMMENT '评论内容',
    `like_count`   INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    `created_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    KEY `idx_note_id` (`note_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论';

-- ============================================================
-- 8. 收藏表 (collection)
-- ============================================================
DROP TABLE IF EXISTS `collection`;
CREATE TABLE `collection` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `note_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '收藏时间',
    UNIQUE KEY `uk_user_note` (`user_id`, `note_id`),
    KEY `idx_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';

-- ============================================================
-- 9. 分享记录表 (share_record)
-- ============================================================
DROP TABLE IF EXISTS `share_record`;
CREATE TABLE `share_record` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分享ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `note_id`    BIGINT UNSIGNED NOT NULL COMMENT '笔记ID',
    `share_type` TINYINT DEFAULT 1 COMMENT '分享类型: 1链接 2图片 3微信',
    `created_at` DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '分享时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分享记录';

-- ============================================================
-- 10. 消息表 (message)
-- ============================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `sender_id`    BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    `receiver_id`  BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `content`      TEXT NOT NULL COMMENT '消息内容',
    `message_type` TINYINT DEFAULT 1 COMMENT '类型: 1文本 2图片',
    `is_read`      TINYINT DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    `created_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    KEY `idx_sender_receiver` (`sender_id`, `receiver_id`),
    KEY `idx_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息';

-- ============================================================
-- 11. 通知表 (notification)
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID',
    `type`        VARCHAR(20) NOT NULL COMMENT '类型: LIKE/COMMENT/FOLLOW/COLLECT/SHARE/SYSTEM',
    `source_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '来源ID',
    `source_type` VARCHAR(20) DEFAULT NULL COMMENT '来源类型',
    `content`     VARCHAR(500) DEFAULT '' COMMENT '通知内容',
    `is_read`     TINYINT DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    `created_at`  DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知';

-- ============================================================
-- 12. 用户等级/积分表 (user_level)
-- ============================================================
DROP TABLE IF EXISTS `user_level`;
CREATE TABLE `user_level` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `level`        INT UNSIGNED DEFAULT 1 COMMENT '当前等级',
    `points`       INT UNSIGNED DEFAULT 0 COMMENT '当前积分',
    `experience`   INT UNSIGNED DEFAULT 0 COMMENT '当前经验值',
    `total_points` INT UNSIGNED DEFAULT 0 COMMENT '累计积分',
    `created_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户等级积分表';

-- ============================================================
-- 13. 用户成就表 (user_achievement)
-- ============================================================
DROP TABLE IF EXISTS `user_achievement`;
CREATE TABLE `user_achievement` (
    `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '成就ID',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `achievement_type` VARCHAR(50) NOT NULL COMMENT '成就类型',
    `achievement_name` VARCHAR(100) NOT NULL COMMENT '成就名称',
    `description`    VARCHAR(500) DEFAULT '' COMMENT '成就描述',
    `icon_url`       VARCHAR(500) DEFAULT '' COMMENT '成就图标URL',
    `obtained_at`    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '获得时间',
    UNIQUE KEY `uk_user_achievement` (`user_id`, `achievement_type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户成就表';

-- ============================================================
-- 14. 积分记录表 (points_record)
-- ============================================================
DROP TABLE IF EXISTS `points_record`;
CREATE TABLE `points_record` (
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `action_type`  VARCHAR(50) NOT NULL COMMENT '行为类型: PUBLISH_NOTE/LIKE_RECEIVED/FOLLOW_RECEIVED/COMMENT',
    `points`       INT NOT NULL COMMENT '积分变化值',
    `description`  VARCHAR(200) DEFAULT '' COMMENT '描述',
    `created_at`   DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- Quiz MVP · V20: 答题题库 / 题目 / 答题记录
-- ============================================================

CREATE TABLE IF NOT EXISTS `quiz_set` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `note_id`         BIGINT UNSIGNED NOT NULL,
    `creator_id`      BIGINT UNSIGNED NOT NULL,
    `title`           VARCHAR(256) NOT NULL DEFAULT '',
    `cover_url`       VARCHAR(512) NOT NULL DEFAULT '',
    `question_count`  INT NOT NULL DEFAULT 10,
    `status`          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `source`          VARCHAR(32) NOT NULL DEFAULT 'AI',
    `prompt_hash`     VARCHAR(128) NOT NULL DEFAULT '',
    `correct_rate`    INT NULL,
    `tags`            JSON NULL,
    `created_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_note_id` (`note_id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_question` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `quiz_set_id`     BIGINT UNSIGNED NOT NULL,
    `sequence_no`     INT NOT NULL,
    `stem`            VARCHAR(512) NOT NULL DEFAULT '',
    `option_a`        VARCHAR(256) NOT NULL DEFAULT '',
    `option_b`        VARCHAR(256) NOT NULL DEFAULT '',
    `option_c`        VARCHAR(256) NOT NULL DEFAULT '',
    `option_d`        VARCHAR(256) NOT NULL DEFAULT '',
    `correct_answer`  VARCHAR(4) NOT NULL,
    `explanation`     VARCHAR(512) NOT NULL DEFAULT '',
    `score`           INT NOT NULL DEFAULT 1,
    `created_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY `uk_quiz_set_seq` (`quiz_set_id`, `sequence_no`),
    KEY `idx_quiz_set_id` (`quiz_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempt` (
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `quiz_set_id`       BIGINT UNSIGNED NOT NULL,
    `user_id`           BIGINT UNSIGNED NOT NULL,
    `game_session_id`    BIGINT UNSIGNED NULL,
    `total_questions`   INT NOT NULL DEFAULT 10,
    `correct_count`     INT NOT NULL DEFAULT 0,
    `score`             INT NOT NULL DEFAULT 0,
    `lives_left`        INT NOT NULL DEFAULT 1,
    `revives_used`      INT NOT NULL DEFAULT 0,
    `status`            VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    `passed`            TINYINT(1) NULL,
    `duration_seconds`  INT NULL,
    `created_at`        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at`        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY `idx_quiz_set_id` (`quiz_set_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_game_session_id` (`game_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
