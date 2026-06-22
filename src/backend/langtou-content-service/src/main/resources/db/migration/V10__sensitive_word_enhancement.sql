-- ============================================================
-- V10: 敏感词表增强
-- 添加 source, status(create_time, update_time) 列及索引
-- ============================================================

-- 添加 source 列（BUILT_IN/CUSTOM）
ALTER TABLE sensitive_word ADD COLUMN source VARCHAR(20) DEFAULT 'BUILT_IN' COMMENT '来源：BUILT_IN-内置 CUSTOM-自定义';

-- 添加 status 列（ENABLED/DISABLED），兼容原有 Integer 类型
-- 先将原有 status 列重命名，再创建新的 VARCHAR 列
ALTER TABLE sensitive_word CHANGE COLUMN status status_old INT DEFAULT 1 COMMENT '旧状态字段';

ALTER TABLE sensitive_word ADD COLUMN status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用 DISABLED-禁用';

-- 迁移旧数据：status_old=1 -> ENABLED, status_old=0 -> DISABLED
UPDATE sensitive_word SET status = CASE WHEN status_old = 1 THEN 'ENABLED' ELSE 'DISABLED' END;

-- 删除旧字段
ALTER TABLE sensitive_word DROP COLUMN status_old;

-- 添加 create_time 列
ALTER TABLE sensitive_word ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 添加 update_time 列
ALTER TABLE sensitive_word ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 添加索引
ALTER TABLE sensitive_word ADD INDEX idx_status (status);
ALTER TABLE sensitive_word ADD INDEX idx_source (source);
ALTER TABLE sensitive_word ADD INDEX idx_word (word);
ALTER TABLE sensitive_word ADD INDEX idx_category (category);
ALTER TABLE sensitive_word ADD INDEX idx_create_time (create_time);
