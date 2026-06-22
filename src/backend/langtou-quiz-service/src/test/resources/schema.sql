-- Quiz MVP · H2 Schema (MySQL 兼容模式)
-- 用于单元测试 / 集成测试

DROP TABLE IF EXISTS quiz_attempt;
DROP TABLE IF EXISTS quiz_question;
DROP TABLE IF EXISTS quiz_set;

CREATE TABLE quiz_set (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id         BIGINT       NOT NULL,
    creator_id      BIGINT,
    title           VARCHAR(255) NOT NULL,
    cover_url       VARCHAR(512),
    question_count  INT          DEFAULT 0,
    status          VARCHAR(32)  NOT NULL,
    source          VARCHAR(64),
    prompt_hash     VARCHAR(128),
    correct_rate    INT,
    tags            VARCHAR(1024),
    created_at      DATETIME,
    updated_at      DATETIME
);

CREATE UNIQUE INDEX uk_quiz_set_note_status
    ON quiz_set (note_id, status);

CREATE TABLE quiz_question (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_set_id     BIGINT       NOT NULL,
    sequence_no     INT          NOT NULL,
    stem            VARCHAR(1024) NOT NULL,
    option_a        VARCHAR(512),
    option_b        VARCHAR(512),
    option_c        VARCHAR(512),
    option_d        VARCHAR(512),
    correct_answer  VARCHAR(128) NOT NULL,
    question_type   VARCHAR(32),
    explanation     VARCHAR(1024),
    score           INT          DEFAULT 1,
    created_at      DATETIME,
    updated_at      DATETIME
);

CREATE UNIQUE INDEX uk_quiz_question_set_seq
    ON quiz_question (quiz_set_id, sequence_no);

CREATE TABLE quiz_attempt (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_set_id         BIGINT      NOT NULL,
    user_id             BIGINT      NOT NULL,
    game_session_id     BIGINT,
    total_questions     INT         DEFAULT 0,
    correct_count       INT         DEFAULT 0,
    score               INT         DEFAULT 0,
    lives_left          INT         DEFAULT 3,
    revives_used        INT         DEFAULT 0,
    status              VARCHAR(32) NOT NULL,
    passed              BOOLEAN,
    duration_seconds    INT,
    version             INT         DEFAULT 0,
    created_at          DATETIME,
    updated_at          DATETIME
);

CREATE UNIQUE INDEX uk_quiz_attempt_game_session
    ON quiz_attempt (game_session_id);
