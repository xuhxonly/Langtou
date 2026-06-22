-- ============================================================
-- V4: 添加地理位置字段到note表
-- 支持LBS附近笔记功能
-- ============================================================

ALTER TABLE `note`
    ADD COLUMN `latitude`  FLOAT DEFAULT NULL COMMENT '纬度' AFTER `location`,
    ADD COLUMN `longitude` FLOAT DEFAULT NULL COMMENT '经度' AFTER `latitude`;

-- 添加空间索引（使用MySQL的空间函数优化附近查询）
-- 注意：MySQL 8.0+ 支持函数索引，这里使用生成列 + 索引的方式
ALTER TABLE `note`
    ADD COLUMN `location_point` POINT GENERATED ALWAYS AS (
        ST_PointFromText(CONCAT('POINT(', `longitude`, ' ', `latitude`, ')'), 4326)
    ) STORED AFTER `longitude`;

-- 创建空间索引
CREATE SPATIAL INDEX `idx_location_point` ON `note` (`location_point`);

-- 为非空经纬度的记录创建普通索引（兜底查询）
CREATE INDEX `idx_lat_lng` ON `note` (`latitude`, `longitude`);
