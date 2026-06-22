-- ============================================================
-- V21__note_quiz_extension.sql
-- 笔记表扩展答题关卡相关字段
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE `note`
    ADD COLUMN `quiz_enabled`   TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否开放为答题关卡：0-否 1-是' AFTER `status`,
    ADD COLUMN `quiz_set_id`   BIGINT UNSIGNED NULL                         COMMENT '关联的题库ID' AFTER `quiz_enabled`,
    ADD COLUMN `quiz_status`    VARCHAR(32)  NOT NULL DEFAULT 'NONE'   COMMENT '答题关卡状态：NONE/GENERATING/READY/EXPIRED' AFTER `quiz_set_id`,
    ADD INDEX `idx_quiz_enabled` (`quiz_enabled`),
    ADD INDEX `idx_quiz_set_id` (`quiz_set_id`);
