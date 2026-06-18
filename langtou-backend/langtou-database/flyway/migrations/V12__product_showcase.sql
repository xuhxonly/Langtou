-- ============================================================
-- V12: 商品橱窗 MVP - 商品、笔记关联商品、商品标签、佣金记录
-- ============================================================

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品价格（元）',
    original_price DECIMAL(10,2) COMMENT '原价（划线价）',
    image_url VARCHAR(500) NOT NULL COMMENT '商品主图URL',
    images JSON COMMENT '商品图片列表（多图）',
    category VARCHAR(50) COMMENT '商品分类',
    external_url VARCHAR(1000) COMMENT '外部购买链接',
    commission_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '佣金比例（%）',
    status VARCHAR(20) DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE-上架/UNAVAILABLE-下架',
    sales_count INT DEFAULT 0 COMMENT '销量（累计）',
    click_count INT DEFAULT 0 COMMENT '点击量（累计）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator_status (creator_id, status),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 商品标签表
CREATE TABLE IF NOT EXISTS product_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_product (product_id),
    INDEX idx_tag (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品标签表';

-- 笔记-商品关联表
CREATE TABLE IF NOT EXISTS note_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    sort_order INT DEFAULT 0 COMMENT '排序（位置）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-移除/1-正常',
    click_count INT DEFAULT 0 COMMENT '该笔记中该商品的点击量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_note_product (note_id, product_id),
    INDEX idx_note (note_id),
    INDEX idx_product (product_id),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记商品关联表';

-- 创作者佣金记录表
CREATE TABLE IF NOT EXISTS creator_commissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    creator_id BIGINT NOT NULL COMMENT '创作者ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    note_id BIGINT COMMENT '关联笔记ID',
    order_no VARCHAR(64) COMMENT '外部订单号',
    buyer_id BIGINT COMMENT '买家ID',
    amount DECIMAL(10,2) COMMENT '订单金额',
    commission_amount DECIMAL(10,2) NOT NULL COMMENT '佣金金额',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING-待确认/SETTLED-已结算/REFUNDED-已退款',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_creator (creator_id),
    INDEX idx_product (product_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者佣金记录表';
