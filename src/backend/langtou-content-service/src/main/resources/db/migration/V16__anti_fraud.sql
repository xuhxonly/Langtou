-- ============================================================
-- V16: 反作弊基础建设 - 设备指纹、举报管理、内容相似度检测
-- ============================================================

-- 1. 设备指纹表
CREATE TABLE IF NOT EXISTS device_fingerprints (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT DEFAULT NULL COMMENT '关联用户ID（未登录时为空）',
    device_id       VARCHAR(128) NOT NULL COMMENT '设备唯一标识',
    device_brand    VARCHAR(64) DEFAULT NULL COMMENT '设备品牌（如 Apple, Huawei）',
    device_model    VARCHAR(64) DEFAULT NULL COMMENT '设备型号（如 iPhone 15, Mate 60）',
    os_type         VARCHAR(32) DEFAULT NULL COMMENT '操作系统类型: IOS/ANDROID/HARMONY',
    os_version      VARCHAR(32) DEFAULT NULL COMMENT '操作系统版本',
    app_version     VARCHAR(32) DEFAULT NULL COMMENT '应用版本号',
    ip_address      VARCHAR(45) DEFAULT NULL COMMENT 'IP地址（支持IPv6）',
    first_seen_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次出现时间',
    last_seen_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    is_blocked      TINYINT NOT NULL DEFAULT 0 COMMENT '是否封禁: 0-正常, 1-封禁',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_device_id (device_id),
    INDEX idx_user (user_id),
    INDEX idx_ip (ip_address),
    INDEX idx_blocked (is_blocked),
    INDEX idx_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备指纹表';

-- 2. 作弊举报表
CREATE TABLE IF NOT EXISTS fraud_reports (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '举报人用户ID',
    fraud_type      VARCHAR(32) NOT NULL COMMENT '作弊类型: LIKE_SPAM/COMMENT_SPAM/FOLLOW_SPAM/CONTENT_DUPLICATE/ACCOUNT_ANOMALY',
    severity        VARCHAR(16) NOT NULL DEFAULT 'LOW' COMMENT '严重程度: LOW/MEDIUM/HIGH/CRITICAL',
    description     VARCHAR(512) DEFAULT NULL COMMENT '举报描述',
    evidence        JSON DEFAULT NULL COMMENT '证据(JSON格式，存储截图URL、数据快照等)',
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING/CONFIRMED/DISMISSED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    processed_at    DATETIME DEFAULT NULL COMMENT '处理时间',
    processor_id    BIGINT DEFAULT NULL COMMENT '处理人(管理员)ID',

    INDEX idx_user (user_id),
    INDEX idx_fraud_type (fraud_type),
    INDEX idx_severity (severity),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_processor (processor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作弊举报表';

-- 3. 内容相似度表
CREATE TABLE IF NOT EXISTS content_similarities (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id_a    BIGINT NOT NULL COMMENT '内容A的ID',
    content_id_b    BIGINT NOT NULL COMMENT '内容B的ID',
    similarity_score DECIMAL(5,4) NOT NULL COMMENT '相似度得分(0.0000~1.0000)',
    check_method    VARCHAR(16) NOT NULL COMMENT '检测方式: TEXT_HASH/IMAGE_HASH/AI',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',

    INDEX idx_content_a (content_id_a),
    INDEX idx_content_b (content_id_b),
    INDEX idx_similarity (similarity_score DESC),
    INDEX idx_method (check_method),
    UNIQUE KEY uk_pair_method (content_id_a, content_id_b, check_method)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容相似度表';
