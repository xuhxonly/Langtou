-- ============================================================
-- V5: 添加埋点事件表
-- 支持用户行为分析（page_view, note_click, like, collect等）
-- ============================================================

CREATE TABLE IF NOT EXISTS `analytics_event` (
    `id`          VARCHAR(64)  NOT NULL COMMENT '事件唯一ID（客户端生成）',
    `user_id`     BIGINT UNSIGNED  DEFAULT NULL COMMENT '用户ID',
    `event_name`  VARCHAR(64)  NOT NULL COMMENT '事件名称: page_view/note_click/note_like/note_collect/note_share/note_comment/search/publish/follow',
    `properties`  JSON         DEFAULT NULL COMMENT '事件动态属性（JSON格式）',
    `created_at`  DATETIME     NOT NULL COMMENT '事件发生时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_event_name` (`event_name`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_user_event` (`user_id`, `event_name`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为埋点事件表';
