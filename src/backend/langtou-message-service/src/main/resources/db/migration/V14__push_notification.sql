-- ============================================================
-- S3-04 消息推送服务 - 数据库迁移
-- 创建推送设备Token表、推送设置表、推送日志表
-- ============================================================

-- 1. 推送设备Token表
CREATE TABLE IF NOT EXISTS push_device_tokens (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    device_type     VARCHAR(16) NOT NULL COMMENT '设备类型: ANDROID/IOS',
    device_token    VARCHAR(512) NOT NULL COMMENT 'FCM/APNs Device Token',
    app_version     VARCHAR(16) DEFAULT NULL COMMENT 'App版本号',
    os_version      VARCHAR(32) DEFAULT NULL COMMENT '操作系统版本',
    last_active_at  DATETIME NOT NULL COMMENT '最后活跃时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_device_token (user_id, device_token),
    INDEX idx_user_id (user_id),
    INDEX idx_device_token (device_token(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送设备Token表';

-- 2. 推送设置表
CREATE TABLE IF NOT EXISTS push_settings (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                 BIGINT NOT NULL COMMENT '用户ID',
    private_message_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '私信推送开关',
    interaction_enabled     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '互动通知推送开关',
    system_enabled          TINYINT(1) NOT NULL DEFAULT 1 COMMENT '系统通知推送开关',
    marketing_enabled       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '营销推送开关',
    quiet_hours_start       VARCHAR(8) NOT NULL DEFAULT '23:00' COMMENT '免打扰开始时间',
    quiet_hours_end         VARCHAR(8) NOT NULL DEFAULT '08:00' COMMENT '免打扰结束时间',
    daily_limit             INT NOT NULL DEFAULT 20 COMMENT '每日推送上限',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户推送设置表';

-- 3. 推送日志表
CREATE TABLE IF NOT EXISTS push_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '目标用户ID',
    device_token    VARCHAR(512) NOT NULL COMMENT '目标设备Token',
    push_type       VARCHAR(32) NOT NULL COMMENT '推送类型: PRIVATE_MESSAGE/INTERACTION/SYSTEM/MARKETING',
    title           VARCHAR(128) NOT NULL COMMENT '推送标题',
    body            VARCHAR(512) NOT NULL COMMENT '推送内容',
    data            JSON DEFAULT NULL COMMENT '推送附加数据',
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SENT/FAILED/DELIVERED',
    sent_at         DATETIME DEFAULT NULL COMMENT '发送时间',
    delivered_at    DATETIME DEFAULT NULL COMMENT '到达时间',
    error_message   VARCHAR(512) DEFAULT NULL COMMENT '失败错误信息',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_status (status),
    INDEX idx_type_time (push_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送日志表';
