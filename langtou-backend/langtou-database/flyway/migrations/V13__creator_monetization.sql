-- ============================================================
-- V13: 创作者变现基础 - 广告收益、钱包、提现
-- ============================================================

-- 创作者广告收益表
CREATE TABLE IF NOT EXISTS creator_ad_revenue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    note_id BIGINT COMMENT '笔记ID',
    ad_type VARCHAR(20) NOT NULL COMMENT '广告类型：IMPRESSION-曝光/CLICK-点击',
    impressions INT DEFAULT 0 COMMENT '曝光次数',
    clicks INT DEFAULT 0 COMMENT '点击次数',
    ctr DECIMAL(5,2) DEFAULT 0.00 COMMENT '点击率（%）',
    revenue DECIMAL(10,4) DEFAULT 0.0000 COMMENT '收益金额（元）',
    settlement_status VARCHAR(20) DEFAULT 'UNSETTLED' COMMENT '结算状态：UNSETTLED-未结算/SETTLED-已结算',
    settlement_date DATE COMMENT '结算日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_creator (creator_id),
    INDEX idx_note (note_id),
    INDEX idx_settlement (settlement_status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者广告收益表';

-- 创作者钱包表
CREATE TABLE IF NOT EXISTS creator_wallet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '钱包ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    total_revenue DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计总收入',
    available_balance DECIMAL(12,2) DEFAULT 0.00 COMMENT '可用余额',
    pending_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '待结算金额',
    withdrawn_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '已提现金额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者钱包表';

-- 提现申请表
CREATE TABLE IF NOT EXISTS withdrawal_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核/APPROVED-已批准/REJECTED-已拒绝/COMPLETED-已完成',
    bank_account VARCHAR(100) COMMENT '银行账号',
    real_name VARCHAR(50) COMMENT '真实姓名',
    requested_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    processed_at DATETIME COMMENT '处理时间',
    remark VARCHAR(500) COMMENT '备注/拒绝原因',
    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_requested (requested_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';
