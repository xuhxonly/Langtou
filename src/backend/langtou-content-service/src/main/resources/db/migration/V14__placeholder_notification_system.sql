﻿﻿﻿﻿﻿-- ============================================================
-- V14 通知与推送系统 (Notification & Push System)
-- 说明: 补全 Flyway 版本号连续性的占位脚本
--       通知服务基础功能已实现，推送设备管理已存在
--       未来版本可在此处添加: 订阅消息模板、推送频率限制、消息已读状态等
-- ============================================================

-- 未来扩展方向:
-- 1. 创建 notification_template 表 (通知模板)
-- 2. 创建 user_notification_subscription 表 (用户订阅设置)
-- 3. 扩展 notification 表增加 read_at (已读时间)
-- 4. 创建 push_frequency_config 表 (推送频率配置)

-- 当前版本为占位脚本，保持空执行以确保 Flyway 版本号连续
SELECT 'V14 migration placeholder executed successfully' AS migration_info;
