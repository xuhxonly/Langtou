-- ============================================================
-- Flyway Migration V7: 添加草稿箱表、笔记扩展字段、敏感词库
-- 榔头(Langtou)社交内容社区APP
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 草稿箱表 (drafts)
-- ============================================================
CREATE TABLE IF NOT EXISTS `drafts` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '草稿ID',
    `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `title`      VARCHAR(200)    NOT NULL DEFAULT '' COMMENT '标题',
    `content`    TEXT            DEFAULT NULL COMMENT '正文内容',
    `images`     JSON            DEFAULT NULL COMMENT '图片URL数组',
    `video_url`  VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '视频URL',
    `tags`       JSON            DEFAULT NULL COMMENT '标签名称数组',
    `location`   VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '发布地点',
    `latitude`   FLOAT           DEFAULT NULL COMMENT '纬度',
    `longitude`  FLOAT           DEFAULT NULL COMMENT '经度',
    `status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-自动保存',
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    KEY `idx_user_id` (`user_id`),
    KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='草稿箱';

-- ============================================================
-- 2. note 表新增字段：可见性、置顶
-- ============================================================
ALTER TABLE `note`
    ADD COLUMN IF NOT EXISTS `visibility` TINYINT NOT NULL DEFAULT 0 COMMENT '可见性：0-公开 1-私密 2-粉丝可见' AFTER `status`;

ALTER TABLE `note`
    ADD COLUMN IF NOT EXISTS `is_pinned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是' AFTER `visibility`;

ALTER TABLE `note`
    ADD COLUMN IF NOT EXISTS `pin_order` INT NOT NULL DEFAULT 0 COMMENT '置顶排序权重（越大越靠前）' AFTER `is_pinned`;

-- ============================================================
-- 3. 敏感词库表 (sensitive_words)
-- ============================================================
CREATE TABLE IF NOT EXISTS `sensitive_words` (
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '敏感词ID',
    `word`       VARCHAR(100)    NOT NULL COMMENT '敏感词内容',
    `category`   VARCHAR(50)     NOT NULL DEFAULT 'custom' COMMENT '分类：custom-自定义 system-系统内置',
    `enabled`    TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用 1-启用',
    `created_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',

    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感词库';
