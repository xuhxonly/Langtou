-- ============================================================
-- V8: 添加数据库索引优化
-- 提升查询性能，覆盖高频查询场景
-- ============================================================

-- users 表索引
ALTER TABLE users ADD INDEX idx_created_at (created_at);

-- notes 表索引
ALTER TABLE notes ADD INDEX idx_updated_at (updated_at);
ALTER TABLE notes ADD INDEX idx_visibility_status (visibility, status, created_at);
ALTER TABLE notes ADD INDEX idx_is_pinned (is_pinned, pin_order);

-- comments 表索引优化
ALTER TABLE comments ADD INDEX idx_note_id_created (note_id, created_at);

-- messages 表索引优化
ALTER TABLE messages ADD INDEX idx_sender_receiver_created (sender_id, receiver_id, created_at);

-- notifications 表索引
ALTER TABLE notifications ADD INDEX idx_user_id_is_read_created (user_id, is_read, created_at);

-- like_records 表索引
ALTER TABLE like_records ADD INDEX idx_target_type (target_type, target_id);

-- drafts 表索引
ALTER TABLE drafts ADD INDEX idx_user_id_updated (user_id, updated_at);

-- reports 表索引
ALTER TABLE reports ADD INDEX idx_status_created (status, created_at);
