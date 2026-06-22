CREATE TABLE IF NOT EXISTS user_skill_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subject VARCHAR(50) NOT NULL COMMENT '学科：math, history, programming, chinese, english, physics, chemistry, biology',
    skill_level DECIMAL(4,2) DEFAULT 5.00 COMMENT '掌握度 1-10',
    confidence DECIMAL(4,2) DEFAULT 0.50 COMMENT 'AI 置信度 0-1',
    trend VARCHAR(10) DEFAULT 'stable' COMMENT 'up/down/stable',
    avg_response_ms INT DEFAULT 30000 COMMENT '平均响应时间(ms)',
    accuracy_rate DECIMAL(4,2) DEFAULT 0.50 COMMENT '准确率 0-1',
    weakness JSON COMMENT '薄弱知识点',
    learning_style JSON COMMENT '学习风格偏好',
    total_questions_answered INT DEFAULT 0,
    total_correct INT DEFAULT 0,
    total_wrong INT DEFAULT 0,
    last_answered_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_subject (user_id, subject),
    INDEX idx_user_id (user_id),
    INDEX idx_subject (subject)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户学科能力画像';

CREATE TABLE IF NOT EXISTS quiz_hint_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_set_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    hint_level INT NOT NULL COMMENT '提示层级 1-4',
    hint_content VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提示请求记录';
