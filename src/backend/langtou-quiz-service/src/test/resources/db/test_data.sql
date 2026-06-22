-- Quiz MVP · 测试数据（仅 H2 / Staging 使用）
-- 3 题库 × 每题 10 题 × 若干答题记录

INSERT INTO quiz_set (id, note_id, creator_id, title, cover_url, question_count, status, source, prompt_hash, correct_rate, created_at, updated_at) VALUES
(1, 1001, 2001, '美妆入门：护肤 10 讲', 'https://cdn.langtou.com/q/1001.jpg', 10, 'PUBLISHED', 'AI', 'hash_1001', 92, NOW(), NOW()),
(2, 1002, 2002, '职场新人 30 天', 'https://cdn.langtou.com/q/1002.jpg', 10, 'READY', 'AI', 'hash_1002', 88, NOW(), NOW()),
(3, 1003, 2001, '情感心理学笔记', 'https://cdn.langtou.com/q/1003.jpg', 10, 'READY', 'TEMPLATE', 'hash_1003', 85, NOW(), NOW());

INSERT INTO quiz_question (quiz_set_id, sequence_no, stem, option_a, option_b, option_c, option_d, correct_answer, explanation, score, created_at, updated_at) VALUES
(1, 1, '以下哪一步不属于晨间护肤？', '洁面', '爽肤水', '晚霜', '防晒', 'C', '晚霜用于夜间修护，不属于晨间步骤。', 1, NOW(), NOW()),
(1, 2, '防晒霜的 SPF 值主要衡量？', '防晒时长', '防晒黑能力', '防水能力', '品牌知名度', 'A', 'SPF 即 Sun Protection Factor，主要衡量防护时长。', 1, NOW(), NOW()),
(2, 1, '新人入职第一周最应该做的是？', '写周报', '认识同事', '了解业务', '申请加薪', 'C', '新人首周核心是理解业务，建立信息源。', 1, NOW(), NOW()),
(2, 2, '会议中被点名提问时，最佳回应是？', '立刻回答', '先确认理解再回答', '沉默', '反问回去', 'B', '先对齐理解可避免答非所问。', 1, NOW(), NOW());

INSERT INTO quiz_attempt (quiz_set_id, user_id, game_session_id, total_questions, correct_count, score, lives_left, revives_used, status, passed, duration_seconds, created_at, updated_at) VALUES
(1, 3001, 5001, 10, 9, 9, 1, 0, 'COMPLETED', 1, 42, NOW(), NOW()),
(1, 3002, 5002, 10, 6, 6, 0, 1, 'COMPLETED', 0, 58, NOW(), NOW()),
(2, 3001, 5003, 10, 10, 10, 1, 0, 'COMPLETED', 1, 30, NOW(), NOW());
