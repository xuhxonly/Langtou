-- ============================================================
-- Flyway Migration V3: 为 note 表添加 video_url 字段
-- 榔头(Langtou)社交内容社区APP
-- ============================================================

USE `langtou`;

-- 为 note 表添加 video_url 字段（如果尚不存在）
ALTER TABLE `note`
    ADD COLUMN IF NOT EXISTS `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频URL' AFTER `images`;

-- 为 note 表添加 cover_url 字段（如果尚不存在，用于视频封面）
ALTER TABLE `note`
    ADD COLUMN IF NOT EXISTS `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面URL' AFTER `video_url`;
