-- ============================================================
-- V17: 数据分析增强 - 内容分析、转化漏斗、创作者每日统计
-- ============================================================

-- 1. 内容分析表（按来源维度统计）
CREATE TABLE IF NOT EXISTS content_analytics (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id          BIGINT NOT NULL COMMENT '内容ID',
    view_source         VARCHAR(32) NOT NULL COMMENT '流量来源: DIRECT/SEARCH/RECOMMEND/HASHTAG/PROFILE/EXTERNAL',
    view_count          BIGINT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    unique_view_count   BIGINT NOT NULL DEFAULT 0 COMMENT '独立浏览次数',
    avg_read_duration   INT NOT NULL DEFAULT 0 COMMENT '平均阅读时长(秒)',
    click_through_rate  DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '点击率',
    share_count         BIGINT NOT NULL DEFAULT 0 COMMENT '分享次数',
    save_count          BIGINT NOT NULL DEFAULT 0 COMMENT '收藏次数',
    date                DATE NOT NULL COMMENT '统计日期',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_content_source_date (content_id, view_source, date),
    INDEX idx_content (content_id),
    INDEX idx_date (date),
    INDEX idx_source (view_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容分析表';

-- 2. 转化漏斗表
CREATE TABLE IF NOT EXISTS traffic_funnel (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_id          BIGINT NOT NULL COMMENT '内容ID',
    impression_count    BIGINT NOT NULL DEFAULT 0 COMMENT '曝光次数',
    click_count         BIGINT NOT NULL DEFAULT 0 COMMENT '点击次数',
    read_count          BIGINT NOT NULL DEFAULT 0 COMMENT '阅读次数',
    interact_count      BIGINT NOT NULL DEFAULT 0 COMMENT '互动次数(点赞+评论+收藏)',
    share_count         BIGINT NOT NULL DEFAULT 0 COMMENT '分享次数',
    date                DATE NOT NULL COMMENT '统计日期',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_content_date (content_id, date),
    INDEX idx_content (content_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转化漏斗表';

-- 3. 创作者每日统计表
CREATE TABLE IF NOT EXISTS creator_daily_stats (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    creator_id          BIGINT NOT NULL COMMENT '创作者ID',
    new_followers       INT NOT NULL DEFAULT 0 COMMENT '新增粉丝',
    unfollowers         INT NOT NULL DEFAULT 0 COMMENT '取关粉丝',
    total_followers     INT NOT NULL DEFAULT 0 COMMENT '总粉丝数',
    content_count       INT NOT NULL DEFAULT 0 COMMENT '发布内容数',
    total_views         BIGINT NOT NULL DEFAULT 0 COMMENT '总浏览量',
    total_likes         BIGINT NOT NULL DEFAULT 0 COMMENT '总点赞数',
    total_comments      BIGINT NOT NULL DEFAULT 0 COMMENT '总评论数',
    total_shares        BIGINT NOT NULL DEFAULT 0 COMMENT '总分享数',
    total_revenue       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '总收入',
    date                DATE NOT NULL COMMENT '统计日期',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_creator_date (creator_id, date),
    INDEX idx_creator (creator_id),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者每日统计表';
