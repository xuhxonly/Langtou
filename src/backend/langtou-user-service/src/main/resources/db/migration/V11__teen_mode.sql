-- ============================================================
-- V11: 青少年模式
-- users表添加青少年模式相关字段
-- contents(note)表添加年龄分级字段
-- 创建 teen_mode_logs 表
-- ============================================================

-- users 表添加青少年模式相关字段
ALTER TABLE user ADD COLUMN age_verified TINYINT DEFAULT 0 COMMENT '是否已验证年龄: 0-未验证 1-已验证';
ALTER TABLE user ADD COLUMN verified_age INT DEFAULT NULL COMMENT '验证后的年龄';
ALTER TABLE user ADD COLUMN teen_mode_enabled TINYINT DEFAULT 0 COMMENT '青少年模式是否开启: 0-关闭 1-开启';
ALTER TABLE user ADD COLUMN teen_mode_pin VARCHAR(64) DEFAULT NULL COMMENT '青少年模式PIN码(加密存储)';
ALTER TABLE user ADD COLUMN daily_usage_seconds INT DEFAULT 0 COMMENT '当日已使用时长(秒)';
ALTER TABLE user ADD COLUMN last_usage_date DATE DEFAULT NULL COMMENT '最后使用日期(用于每日重置)';

-- contents(note)表添加年龄分级字段
ALTER TABLE note ADD COLUMN age_rating VARCHAR(10) DEFAULT 'ALL' COMMENT '年龄分级: ALL-全年龄 7+-7岁以上 12+-12岁以上 18+-18岁以上';

-- 创建 teen_mode_logs 表（记录使用时长日志）
CREATE TABLE IF NOT EXISTS teen_mode_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    usage_date DATE NOT NULL COMMENT '使用日期',
    total_seconds INT NOT NULL DEFAULT 0 COMMENT '当日总使用时长(秒)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    UNIQUE KEY uk_user_date (user_id, usage_date),
    INDEX idx_user_id (user_id),
    INDEX idx_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='青少年模式使用时长日志表';

-- users 表索引
ALTER TABLE user ADD INDEX idx_teen_mode_enabled (teen_mode_enabled);
ALTER TABLE user ADD INDEX idx_age_verified (age_verified);
