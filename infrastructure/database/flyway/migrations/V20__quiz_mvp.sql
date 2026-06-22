-- ============================================================
-- V20__quiz_mvp.sql
-- 榔头 AI + UGC 互动答题 / 知识闯关 MVP 核心表结构
-- ============================================================

SET NAMES utf8mb4;

-- 题库表（QuizSet）：每一条"笔记 → AI 关卡"的生成结果
CREATE TABLE IF NOT EXISTS `quiz_set` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '题库ID',
    `note_id`         BIGINT UNSIGNED NOT NULL                      COMMENT '关联笔记ID',
    `creator_id`      BIGINT UNSIGNED NOT NULL                      COMMENT '创作者ID',
    `title`           VARCHAR(256)  NOT NULL DEFAULT ''             COMMENT '关卡标题（通常与笔记标题一致）',
    `cover_url`       VARCHAR(512)  NOT NULL DEFAULT ''             COMMENT '封面图（取笔记封面）',
    `question_count`  INT           NOT NULL DEFAULT 10            COMMENT '题目数量（MVP 固定 10）',
    `status`          VARCHAR(32)   NOT NULL DEFAULT 'PENDING'     COMMENT '状态：PENDING/READY/FAILED/EXPIRED/PUBLISHED',
    `source`          VARCHAR(32)   NOT NULL DEFAULT 'AI'           COMMENT '来源：AI/TEMPLATE',
    `prompt_hash`     VARCHAR(128)  NOT NULL DEFAULT ''            COMMENT 'AI 原始 Prompt 的 Hash（脱敏，供对账）',
    `correct_rate`    INT           NULL                          COMMENT '题库正确率（百分制）',
    `tags`            JSON          NULL                          COMMENT '标签列表（冗余，加速检索）',
    `created_at`      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY `uk_note_id` (`note_id`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题题库表（AI 生成的关卡）';

-- 题目表（QuizQuestion）：每道选择题
CREATE TABLE IF NOT EXISTS `quiz_question` (
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    `quiz_set_id`     BIGINT UNSIGNED NOT NULL                      COMMENT '所属题库ID',
    `sequence_no`     INT           NOT NULL                      COMMENT '题目序号（从 1 开始）',
    `stem`            VARCHAR(512)  NOT NULL DEFAULT ''             COMMENT '题干（20–60 字）',
    `option_a`        VARCHAR(256)  NOT NULL DEFAULT ''             COMMENT '选项 A',
    `option_b`        VARCHAR(256)  NOT NULL DEFAULT ''             COMMENT '选项 B',
    `option_c`        VARCHAR(256)  NOT NULL DEFAULT ''             COMMENT '选项 C',
    `option_d`        VARCHAR(256)  NOT NULL DEFAULT ''             COMMENT '选项 D',
    `correct_answer`  VARCHAR(4)    NOT NULL                       COMMENT '正确答案（A/B/C/D）',
    `explanation`     VARCHAR(512)  NOT NULL DEFAULT ''             COMMENT '答案解析',
    `score`           INT           NOT NULL DEFAULT 1             COMMENT '分值',
    `created_at`      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY `uk_quiz_set_seq` (`quiz_set_id`, `sequence_no`),
    KEY `idx_quiz_set_id` (`quiz_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题题目表';

-- 答题记录表（QuizAttempt）：玩家一次闯关的完整记录
CREATE TABLE IF NOT EXISTS `quiz_attempt` (
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '答题记录ID',
    `quiz_set_id`       BIGINT UNSIGNED NOT NULL                  COMMENT '题库ID',
    `user_id`           BIGINT UNSIGNED NOT NULL                  COMMENT '玩家用户ID',
    `game_session_id`    BIGINT UNSIGNED NULL                      COMMENT '关联 Game Session ID',
    `total_questions`   INT           NOT NULL DEFAULT 10        COMMENT '总题数',
    `correct_count`     INT           NOT NULL DEFAULT 0         COMMENT '答对题数',
    `score`             INT           NOT NULL DEFAULT 0         COMMENT '总得分',
    `lives_left`        INT           NOT NULL DEFAULT 1         COMMENT '剩余生命数',
    `revives_used`      INT           NOT NULL DEFAULT 0         COMMENT '已使用续命次数',
    `status`            VARCHAR(32)   NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态：IN_PROGRESS/COMPLETED/ABANDONED',
    `passed`            TINYINT(1)    NULL                       COMMENT '是否通关',
    `duration_seconds`  INT           NULL                       COMMENT '总耗时（秒）',
    `created_at`        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at`        DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    KEY `idx_quiz_set_id` (`quiz_set_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_game_session_id` (`game_session_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家答题记录表';
