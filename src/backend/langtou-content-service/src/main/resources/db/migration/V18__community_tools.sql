-- ============================================================
-- V18: 社区运营工具 - 用户等级、成就系统、新手引导
-- ============================================================

-- 1. 用户等级表
CREATE TABLE IF NOT EXISTS user_levels (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    level               INT NOT NULL COMMENT '等级',
    name                VARCHAR(64) NOT NULL COMMENT '等级名称',
    icon_url            VARCHAR(512) DEFAULT NULL COMMENT '等级图标URL',
    min_points          INT NOT NULL DEFAULT 0 COMMENT '最低积分',
    max_points          INT NOT NULL DEFAULT 0 COMMENT '最高积分',
    privileges          JSON DEFAULT NULL COMMENT '等级特权(JSON)',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_level (level),
    INDEX idx_points_range (min_points, max_points)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户等级表';

-- 2. 用户成就表
CREATE TABLE IF NOT EXISTS user_achievements (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    achievement_type    VARCHAR(64) NOT NULL COMMENT '成就类型: FIRST_NOTE/FIRST_COMMENT/LIKE_MILESTONE/FOLLOWER_MILESTONE/CONTINUOUS_LOGIN/CONTENT_MILESTONE',
    achievement_name    VARCHAR(128) NOT NULL COMMENT '成就名称',
    description         VARCHAR(512) DEFAULT NULL COMMENT '成就描述',
    icon_url            VARCHAR(512) DEFAULT NULL COMMENT '成就图标URL',
    unlocked_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',

    UNIQUE KEY uk_user_type (user_id, achievement_type),
    INDEX idx_user (user_id),
    INDEX idx_type (achievement_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成就表';

-- 3. 新手引导步骤表
CREATE TABLE IF NOT EXISTS onboarding_steps (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    step_order          INT NOT NULL COMMENT '步骤顺序',
    title               VARCHAR(128) NOT NULL COMMENT '步骤标题',
    description         VARCHAR(512) DEFAULT NULL COMMENT '步骤描述',
    image_url           VARCHAR(512) DEFAULT NULL COMMENT '步骤图片URL',
    action_type         VARCHAR(64) NOT NULL COMMENT '动作类型: SELECT_INTERESTS/FOLLOW_CREATORS/PUBLISH_FIRST_NOTE/COMPLETE_PROFILE',
    target_url          VARCHAR(512) DEFAULT NULL COMMENT '目标跳转URL',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_step_order (step_order),
    INDEX idx_action_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新手引导步骤表';

-- 4. 用户引导完成记录表
CREATE TABLE IF NOT EXISTS user_onboarding_progress (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    step_id             BIGINT NOT NULL COMMENT '引导步骤ID',
    completed           TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
    completed_at        DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_step (user_id, step_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户引导完成记录表';
