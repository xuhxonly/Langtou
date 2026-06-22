-- ============================================================
-- Flyway Migration V6: 广告系统表
-- 榔头(Langtou)社交内容社区APP - 商业化基础框架
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. 广告表 (advertisement)
-- ============================================================
CREATE TABLE IF NOT EXISTS `advertisement` (
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '广告ID',
    `advertiser_id`  BIGINT UNSIGNED NOT NULL COMMENT '广告主ID',
    `title`          VARCHAR(200)    NOT NULL COMMENT '广告标题',
    `image_url`      VARCHAR(512)    NOT NULL DEFAULT '' COMMENT '广告图片URL',
    `target_url`     VARCHAR(1024)   NOT NULL DEFAULT '' COMMENT '点击跳转目标URL',
    `ad_type`        VARCHAR(16)     NOT NULL DEFAULT 'feed' COMMENT '广告类型：feed-信息流 banner-横幅 splash-开屏',
    `position`       INT             NOT NULL DEFAULT 0 COMMENT '广告位权重（越大优先级越高）',
    `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-投放中 2-审核中',
    `start_time`     DATETIME(3)     NOT NULL COMMENT '投放开始时间',
    `end_time`       DATETIME(3)     NOT NULL COMMENT '投放结束时间',
    `budget`         DECIMAL(12,2)   NOT NULL DEFAULT 0.00 COMMENT '广告预算（元）',
    `impressions`    INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '曝光次数',
    `clicks`         INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '点击次数',
    `created_at`     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',

    KEY `idx_ad_type_status` (`ad_type`, `status`, `start_time`, `end_time`),
    KEY `idx_advertiser_id` (`advertiser_id`),
    KEY `idx_position` (`position` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告表';

-- ============================================================
-- 2. 广告曝光记录表 (ad_impression)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ad_impression` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '曝光记录ID',
    `ad_id`       BIGINT UNSIGNED NOT NULL COMMENT '广告ID',
    `user_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID（未登录为0）',
    `note_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联笔记ID（信息流广告上下文）',
    `position`    INT             NOT NULL DEFAULT 0 COMMENT '展示位置（第几条）',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '曝光时间',

    KEY `idx_ad_id` (`ad_id`, `created_at`),
    KEY `idx_user_id` (`user_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告曝光记录表';

-- ============================================================
-- 3. 广告点击记录表 (ad_click)
-- ============================================================
CREATE TABLE IF NOT EXISTS `ad_click` (
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点击记录ID',
    `ad_id`       BIGINT UNSIGNED NOT NULL COMMENT '广告ID',
    `user_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '用户ID（未登录为0）',
    `note_id`     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '关联笔记ID（信息流广告上下文）',
    `created_at`  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '点击时间',

    KEY `idx_ad_id` (`ad_id`, `created_at`),
    KEY `idx_user_id` (`user_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='广告点击记录表';
