-- ============================================================
-- V15: 运营活动后台 - 活动管理、官方账号、推荐位
-- ============================================================

-- 1. 活动表
CREATE TABLE IF NOT EXISTS activities (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    title               VARCHAR(64) NOT NULL COMMENT '活动标题',
    description         TEXT NOT NULL COMMENT '活动描述',
    cover_url           VARCHAR(512) NOT NULL COMMENT '活动封面图URL',
    creator_id          BIGINT NOT NULL COMMENT '创建者/运营人员ID',
    type                VARCHAR(32) NOT NULL DEFAULT 'CHALLENGE' COMMENT '活动类型: CHALLENGE/TOPIC/EVENT',
    status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/ONLINE/ENDED/REJECTED',
    start_time          DATETIME NOT NULL COMMENT '活动开始时间',
    end_time            DATETIME NOT NULL COMMENT '活动结束时间',
    participation_rules JSON DEFAULT NULL COMMENT '参与规则(JSON)',
    reward_config       JSON DEFAULT NULL COMMENT '奖励配置(JSON)',
    participant_count   INT NOT NULL DEFAULT 0 COMMENT '参与人数(冗余)',
    note_count          INT NOT NULL DEFAULT 0 COMMENT '参与笔记数(冗余)',
    total_views         BIGINT NOT NULL DEFAULT 0 COMMENT '总浏览量(冗余)',
    total_interactions  BIGINT NOT NULL DEFAULT 0 COMMENT '总互动量(冗余)',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_type (type),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 2. 活动参与记录表
CREATE TABLE IF NOT EXISTS activity_participants (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    user_id         BIGINT NOT NULL COMMENT '参与用户ID',
    note_count      INT NOT NULL DEFAULT 0 COMMENT '参与笔记数',
    joined_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '参与时间',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_activity_user (activity_id, user_id),
    INDEX idx_activity (activity_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与记录表';

-- 3. 活动标签绑定表
CREATE TABLE IF NOT EXISTS activity_tags (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    tag_name        VARCHAR(32) NOT NULL COMMENT '标签名称',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_activity_tag (activity_id, tag_name),
    INDEX idx_activity (activity_id),
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动标签绑定表';

-- 4. 官方账号表
CREATE TABLE IF NOT EXISTS official_accounts (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '关联用户ID',
    account_type    VARCHAR(32) NOT NULL COMMENT '账号类型: OFFICIAL/VERIFIED_CREATOR/BRAND',
    display_name    VARCHAR(64) NOT NULL COMMENT '显示名称',
    description     VARCHAR(256) DEFAULT NULL COMMENT '账号简介',
    avatar_url      VARCHAR(512) NOT NULL COMMENT '头像URL',
    verified_badge  VARCHAR(16) NOT NULL DEFAULT 'BLUE_V' COMMENT '认证标识: BLUE_V/GOLD_V',
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user (user_id),
    INDEX idx_type (account_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='官方账号表';

-- 5. 推荐位管理表
CREATE TABLE IF NOT EXISTS recommend_positions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_type   VARCHAR(32) NOT NULL COMMENT '推荐位类型: HOME_BANNER/DISCOVER/TOPIC_PAGE/SEARCH/ONBOARDING',
    title           VARCHAR(128) DEFAULT NULL COMMENT '展示标题',
    content         JSON DEFAULT NULL COMMENT '内容配置(JSON)',
    image_url       VARCHAR(512) DEFAULT NULL COMMENT '展示图片URL',
    link_url        VARCHAR(512) DEFAULT NULL COMMENT '跳转链接',
    sort_order      INT NOT NULL DEFAULT 0 COMMENT '排序(越大越靠前)',
    start_time      DATETIME DEFAULT NULL COMMENT '展示开始时间',
    end_time        DATETIME DEFAULT NULL COMMENT '展示结束时间',
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_position_status (position_type, status, sort_order DESC),
    INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐位管理表';
