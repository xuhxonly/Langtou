-- ============================================================
-- V9: 添加外键约束
-- 增强数据完整性，确保关联关系有效性
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- notes.user_id -> users.id
ALTER TABLE notes
    ADD CONSTRAINT fk_notes_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- comments.user_id -> users.id
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- comments.note_id -> notes.id
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_note_id
    FOREIGN KEY (note_id) REFERENCES notes(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- follows.follower_id -> users.id
ALTER TABLE follows
    ADD CONSTRAINT fk_follows_follower_id
    FOREIGN KEY (follower_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- follows.following_id -> users.id
ALTER TABLE follows
    ADD CONSTRAINT fk_follows_following_id
    FOREIGN KEY (following_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- likes.user_id -> users.id
ALTER TABLE likes
    ADD CONSTRAINT fk_likes_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- collections.user_id -> users.id
ALTER TABLE collections
    ADD CONSTRAINT fk_collections_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- collections.note_id -> notes.id
ALTER TABLE collections
    ADD CONSTRAINT fk_collections_note_id
    FOREIGN KEY (note_id) REFERENCES notes(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- messages.sender_id -> users.id
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_sender_id
    FOREIGN KEY (sender_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- messages.receiver_id -> users.id
ALTER TABLE messages
    ADD CONSTRAINT fk_messages_receiver_id
    FOREIGN KEY (receiver_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- notifications.user_id -> users.id
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- note_tags.note_id -> notes.id
ALTER TABLE note_tags
    ADD CONSTRAINT fk_note_tags_note_id
    FOREIGN KEY (note_id) REFERENCES notes(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- note_tags.tag_id -> tags.id
ALTER TABLE note_tags
    ADD CONSTRAINT fk_note_tags_tag_id
    FOREIGN KEY (tag_id) REFERENCES tags(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

-- drafts.user_id -> users.id
ALTER TABLE drafts
    ADD CONSTRAINT fk_drafts_user_id
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;
