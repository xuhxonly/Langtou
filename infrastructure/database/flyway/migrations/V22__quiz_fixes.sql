-- ============================================================
-- V22__quiz_fixes.sql
-- 「魔鬼辩护人」审查报告第一优先级修复
-- 涉及：唯一键扩展、correct_answer 扩宽、新增 question_type、状态 CHECK、乐观锁
-- ============================================================

SET NAMES utf8mb4;

-- 1. quiz_set: 唯一键由 (note_id) 扩展为 (note_id, creator_id)
--    允许同一笔记被不同创作者各自生成题库；仅 note_id 单列会互相冲突
ALTER TABLE `quiz_set`
    DROP INDEX `uk_note_id`,
    ADD UNIQUE KEY `uk_note_creator` (`note_id`, `creator_id`);

-- 2. quiz_question: correct_answer 由 VARCHAR(4) 扩为 VARCHAR(64)，支持多选（如 "A,C"）
ALTER TABLE `quiz_question`
    MODIFY COLUMN `correct_answer` VARCHAR(64) NOT NULL COMMENT '正确答案（单选 A/B/C/D，多选以逗号分隔如 A,C）';

-- 3. quiz_question: 新增 question_type 字段（SINGLE / MULTI / JUDGE）
ALTER TABLE `quiz_question`
    ADD COLUMN `question_type` VARCHAR(16) NOT NULL DEFAULT 'SINGLE' COMMENT '题型：SINGLE-单选 / MULTI-多选 / JUDGE-判断' AFTER `correct_answer`,
    ADD INDEX `idx_quiz_set_seq_type` (`quiz_set_id`, `sequence_no`, `question_type`);

-- 4. quiz_attempt: status 增加 CHECK 约束，防止非法状态写入
ALTER TABLE `quiz_attempt`
    ADD CONSTRAINT `chk_quiz_attempt_status`
        CHECK (`status` IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'));

-- 5. quiz_attempt: 新增 version 字段，供 MyBatis-Plus @Version 乐观锁使用
ALTER TABLE `quiz_attempt`
    ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号（MyBatis-Plus @Version）' AFTER `duration_seconds`;
