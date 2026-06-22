-- Quiz MVP · 测试初始化数据
-- 4 题库 × 若干题 × 若干答题记录
-- 唯一约束: (note_id, status) — 相同 note_id 下每个状态唯一

INSERT INTO quiz_set (id, note_id, creator_id, title, cover_url, question_count, status, source, prompt_hash, correct_rate, tags, created_at, updated_at) VALUES
(1, 1001, 2001, '美妆入门：护肤10讲', 'https://cdn.langtou.com/q/1001.jpg', 10, 'PUBLISHED', 'AI', 'hash_1001', 92, '["美妆","护肤","入门"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1002, 2002, '职场新人30天', 'https://cdn.langtou.com/q/1002.jpg', 10, 'READY', 'AI', 'hash_1002', 88, '["职场","新人"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1003, 2001, '情感心理学笔记', 'https://cdn.langtou.com/q/1003.jpg', 10, 'READY', 'TEMPLATE', 'hash_1003', 85, '["情感","心理"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1001, 2001, '美妆入门：护肤10讲V2', 'https://cdn.langtou.com/q/1001_v2.jpg', 10, 'READY', 'AI', 'hash_1001_v2', 90, '["美妆","护肤"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO quiz_question (quiz_set_id, sequence_no, stem, option_a, option_b, option_c, option_d, correct_answer, question_type, explanation, score, created_at, updated_at) VALUES
(1, 1, '以下哪一步不属于晨间护肤？', '洁面', '爽肤水', '晚霜', '防晒', 'C', 'SINGLE', '晚霜用于夜间修护，不属于晨间步骤。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 2, '防晒霜的SPF值主要衡量？', '防晒时长', '防晒黑能力', '防水能力', '品牌知名度', 'A', 'SINGLE', 'SPF即Sun Protection Factor，主要衡量防护时长。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 3, '以下哪些属于护肤的活性成分？', '玻尿酸', '视黄醇', '二氧化钛', '烟酰胺', '["A","B","D"]', 'MULTI', '视黄醇、烟酰胺是活性成分。', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '新人入职第一周最应该做的是？', '写周报', '认识同事', '了解业务', '申请加薪', 'C', 'SINGLE', '新人首周核心是理解业务，建立信息源。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, '会议中被点名提问时，最佳回应是？', '立刻回答', '先确认理解再回答', '沉默', '反问回去', 'B', 'SINGLE', '先对齐理解可避免答非所问。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, '情绪ABC理论中A代表什么？', '情绪', '事件', '信念', '结果', 'B', 'SINGLE', 'A=Activating Event 激发事件。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, '美妆V2 Q1：面膜应该敷多久？', '5分钟', '15分钟', '45分钟', '一整晚', 'B', 'SINGLE', '常规面膜建议15分钟。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, '美妆V2 Q2：以下说法是否正确？防晒霜需要卸妆。', '正确', '错误', NULL, NULL, 'A', 'JUDGE', '防水型防晒霜需要卸妆。', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO quiz_attempt (quiz_set_id, user_id, game_session_id, total_questions, correct_count, score, lives_left, revives_used, status, passed, duration_seconds, version, created_at, updated_at) VALUES
(1, 3001, 5001, 10, 9, 9, 1, 0, 'COMPLETED', TRUE, 42, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 3002, 5002, 10, 6, 6, 0, 1, 'COMPLETED', FALSE, 58, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 3001, 5003, 10, 10, 10, 1, 0, 'COMPLETED', TRUE, 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 3003, 5004, 10, 0, 0, 3, 0, 'IN_PROGRESS', NULL, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
