﻿﻿﻿﻿﻿-- V19__game_service_schema.sql
-- 榔头平台游戏化服务核心表结构

CREATE TABLE IF NOT EXISTS game_session (
    id              BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    game_id         BIGINT          NOT NULL                    COMMENT '游戏ID',
    room_id         VARCHAR(64)     NOT NULL                    COMMENT '房间ID',
    host_user_id    BIGINT          NOT NULL                    COMMENT '房主用户ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'WAITING'  COMMENT '状态：WAITING/IN_PROGRESS/FINISHED/CANCELLED',
    started_at      DATETIME        NULL                        COMMENT '开始时间',
    ended_at        DATETIME        NULL                        COMMENT '结束时间',
    max_players     INT             NOT NULL DEFAULT 8          COMMENT '最大玩家数',
    current_players INT             NOT NULL DEFAULT 1          COMMENT '当前玩家数',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_game_id (game_id),
    KEY idx_host_user_id (host_user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏对局表';

CREATE TABLE IF NOT EXISTS game_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    name            VARCHAR(128)    NOT NULL                    COMMENT '道具名称',
    type            VARCHAR(64)     NOT NULL                    COMMENT '道具类型',
    rarity          VARCHAR(32)     NOT NULL                    COMMENT '稀有度',
    description     VARCHAR(512)    NULL                        COMMENT '道具描述',
    icon_url        VARCHAR(512)    NULL                        COMMENT '图标URL',
    stackable       TINYINT(1)      NOT NULL DEFAULT 1          COMMENT '是否可叠加',
    max_stack       INT             NOT NULL DEFAULT 99         COMMENT '最大叠加数',
    price           DECIMAL(12,2)   NULL                        COMMENT '价格',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_type (type),
    KEY idx_rarity (rarity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏道具表';

CREATE TABLE IF NOT EXISTS game_inventory (
    id              BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    user_id         BIGINT          NOT NULL                    COMMENT '用户ID',
    item_id         BIGINT          NOT NULL                    COMMENT '道具ID',
    item_type       VARCHAR(64)     NULL                        COMMENT '道具类型快照',
    quantity        INT             NOT NULL DEFAULT 1          COMMENT '数量',
    equipped        TINYINT(1)      NOT NULL DEFAULT 0          COMMENT '是否装备',
    expires_at      DATETIME        NULL                        COMMENT '过期时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_item_id (item_id),
    KEY idx_user_item (user_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='玩家背包表';

CREATE TABLE IF NOT EXISTS game_matchmaking (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    user_id             BIGINT          NOT NULL                    COMMENT '用户ID',
    game_id             BIGINT          NOT NULL                    COMMENT '游戏ID',
    mmr                 INT             NOT NULL DEFAULT 1000       COMMENT 'MMR 匹配分',
    queue_type          VARCHAR(32)     NOT NULL DEFAULT 'RANKED'   COMMENT '队列类型：RANKED/CASUAL',
    status              VARCHAR(32)     NOT NULL DEFAULT 'QUEUED'   COMMENT '状态：QUEUED/MATCHED/CANCELLED/COMPLETED',
    expected_wait_time  INT             NULL                        COMMENT '预期等待时长（秒）',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_game_id (game_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配记录表';

CREATE TABLE IF NOT EXISTS game_leaderboard (
    id          BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    game_id     BIGINT          NOT NULL                    COMMENT '游戏ID',
    user_id     BIGINT          NOT NULL                    COMMENT '用户ID',
    score       INT             NOT NULL DEFAULT 0          COMMENT '分数',
    rank        INT             NULL                        COMMENT '排名',
    season_id   BIGINT          NULL                        COMMENT '赛季ID',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_game_user_season (game_id, user_id, season_id),
    KEY idx_score (score DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜表';

CREATE TABLE IF NOT EXISTS game_season (
    id          BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    name        VARCHAR(128)    NOT NULL                    COMMENT '赛季名称',
    start_date  DATETIME        NOT NULL                    COMMENT '开始时间',
    end_date    DATETIME        NOT NULL                    COMMENT '结束时间',
    status      VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE'   COMMENT '状态：ACTIVE/ENDED/UPCOMING',
    description VARCHAR(512)    NULL                        COMMENT '赛季描述',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛季表';

CREATE TABLE IF NOT EXISTS game_quest (
    id              BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    game_id         BIGINT          NOT NULL                    COMMENT '游戏ID',
    title           VARCHAR(256)    NOT NULL                    COMMENT '任务标题',
    description     VARCHAR(512)    NULL                        COMMENT '任务描述',
    type            VARCHAR(32)     NOT NULL                    COMMENT '任务类型：DAILY/WEEKLY/ACHIEVEMENT',
    target_value    INT             NOT NULL DEFAULT 1          COMMENT '目标值',
    reward_points   INT             NOT NULL DEFAULT 0          COMMENT '奖励积分',
    reward_item_id  BIGINT          NULL                        COMMENT '奖励道具ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE'   COMMENT '状态：ACTIVE/CLAIMED/DISABLED',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_game_id (game_id),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务/成就表';

CREATE TABLE IF NOT EXISTS game_payment (
    id                  BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    user_id             BIGINT          NOT NULL                    COMMENT '用户ID',
    order_no            VARCHAR(64)     NOT NULL                    COMMENT '订单号',
    product_id          VARCHAR(128)    NOT NULL                    COMMENT '商品ID',
    amount              BIGINT          NOT NULL                    COMMENT '金额（单位：分）',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY'      COMMENT '币种',
    channel             VARCHAR(32)     NOT NULL                    COMMENT '支付渠道',
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING'  COMMENT '状态：PENDING/SUCCESS/FAILED/REFUNDED',
    paid_at             DATETIME        NULL                        COMMENT '支付时间',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_status (status),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='游戏支付订单表';

CREATE TABLE IF NOT EXISTS game_session_players (
    id          BIGINT      NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
    session_id  BIGINT      NOT NULL                    COMMENT '对局ID',
    user_id     BIGINT      NOT NULL                    COMMENT '玩家用户ID',
    joined_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_user (session_id, user_id),
    KEY idx_session_id (session_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对局玩家关联表';
