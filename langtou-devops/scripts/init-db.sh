#!/bin/bash
# ==========================================
# Langtou 社交内容社区 APP - 数据库初始化脚本
# ==========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 数据库配置
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_ROOT_USER="${DB_ROOT_USER:-root}"
DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-Langtou@Root2024}"
DB_NAME="${DB_NAME:-langtou}"
DB_USER="${DB_USER:-langtou}"
DB_PASSWORD="${DB_PASSWORD:-Langtou@Db2024}"

# 打印带颜色的信息
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查MySQL连接
check_mysql() {
    log_info "检查 MySQL 连接..."
    
    if ! command -v mysql &> /dev/null; then
        log_error "MySQL 客户端未安装"
        exit 1
    fi
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" -e "SELECT 1" &> /dev/null; then
            log_success "MySQL 连接成功"
            return 0
        fi
        
        log_warn "等待 MySQL 就绪... (尝试 $attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "无法连接到 MySQL"
    exit 1
}

# 创建数据库和用户
init_database() {
    log_info "初始化数据库..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" <<EOF
-- 创建数据库
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 创建业务用户
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';

-- 创建Nacos数据库
CREATE DATABASE IF NOT EXISTS \`nacos\` 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON \`nacos\`.* TO '${DB_USER}'@'%';

FLUSH PRIVILEGES;
EOF
    
    log_success "数据库和用户创建完成"
}

# 创建业务表
init_tables() {
    log_info "创建业务表..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<'EOF'
-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    `birthday` DATE DEFAULT NULL COMMENT '生日',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 内容表 (帖子/笔记)
CREATE TABLE IF NOT EXISTS `contents` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内容ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '作者ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-图文 2-视频',
    `cover` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
    `images` JSON DEFAULT NULL COMMENT '图片列表',
    `video_url` VARCHAR(500) DEFAULT NULL COMMENT '视频URL',
    `tags` JSON DEFAULT NULL COMMENT '标签',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '位置',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-草稿 1-已发布 2-审核中 3-已删除',
    `view_count` INT UNSIGNED DEFAULT 0 COMMENT '浏览数',
    `like_count` INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT UNSIGNED DEFAULT 0 COMMENT '评论数',
    `collect_count` INT UNSIGNED DEFAULT 0 COMMENT '收藏数',
    `share_count` INT UNSIGNED DEFAULT 0 COMMENT '分享数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_type` (`type`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容表';

-- 互动表 (点赞)
CREATE TABLE IF NOT EXISTS `likes` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
    `target_type` TINYINT NOT NULL DEFAULT 1 COMMENT '目标类型: 1-内容 2-评论',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`, `target_type`),
    KEY `idx_target` (`target_id`, `target_type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点赞表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comments` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `content_id` BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
    `parent_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '父评论ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `like_count` INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-删除 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_content_id` (`content_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `collections` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `content_id` BIGINT UNSIGNED NOT NULL COMMENT '内容ID',
    `folder_id` BIGINT UNSIGNED DEFAULT 0 COMMENT '收藏夹ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_content` (`user_id`, `content_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_content_id` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 关注表
CREATE TABLE IF NOT EXISTS `follows` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `follower_id` BIGINT UNSIGNED NOT NULL COMMENT '关注者ID',
    `following_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注者ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-取消关注 1-关注中',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    KEY `idx_follower` (`follower_id`),
    KEY `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='关注表';

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `sender_id` BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    `type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-私信 2-系统通知 3-点赞通知 4-评论通知',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `extra` JSON DEFAULT NULL COMMENT '额外信息',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_receiver` (`receiver_id`, `is_read`),
    KEY `idx_sender` (`sender_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 会话表
CREATE TABLE IF NOT EXISTS `conversations` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '对方ID',
    `type` TINYINT DEFAULT 1 COMMENT '类型: 1-单聊 2-群聊',
    `last_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '最后消息ID',
    `last_message_content` TEXT COMMENT '最后消息内容',
    `unread_count` INT UNSIGNED DEFAULT 0 COMMENT '未读数',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';
EOF
    
    log_success "业务表创建完成"
}

# 初始化Nacos数据库
init_nacos_db() {
    log_info "初始化 Nacos 数据库..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" nacos <<'EOF'
-- Nacos 配置表
CREATE TABLE IF NOT EXISTS `config_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) DEFAULT NULL,
    `content` LONGTEXT NOT NULL,
    `md5` VARCHAR(32) DEFAULT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `src_user` TEXT,
    `src_ip` VARCHAR(50) DEFAULT NULL,
    `app_name` VARCHAR(128) DEFAULT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `c_desc` VARCHAR(256) DEFAULT NULL,
    `c_use` VARCHAR(64) DEFAULT NULL,
    `effect` VARCHAR(64) DEFAULT NULL,
    `type` VARCHAR(64) DEFAULT NULL,
    `c_schema` TEXT,
    `encrypted_data_key` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfo_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `config_info_aggr` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) NOT NULL,
    `datum_id` VARCHAR(255) NOT NULL,
    `content` LONGTEXT NOT NULL,
    `gmt_modified` DATETIME NOT NULL,
    `app_name` VARCHAR(128) DEFAULT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfoaggr_datagrouptenantdatum` (`data_id`,`group_id`,`tenant_id`,`datum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `config_info_beta` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) NOT NULL,
    `app_name` VARCHAR(128) DEFAULT NULL,
    `content` LONGTEXT NOT NULL,
    `beta_ips` VARCHAR(1024) DEFAULT NULL,
    `md5` VARCHAR(32) DEFAULT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `src_user` TEXT,
    `src_ip` VARCHAR(50) DEFAULT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `encrypted_data_key` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfobeta_datagrouptenant` (`data_id`,`group_id`,`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `config_info_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) NOT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `tag_id` VARCHAR(128) NOT NULL,
    `app_name` VARCHAR(128) DEFAULT NULL,
    `content` LONGTEXT NOT NULL,
    `md5` VARCHAR(32) DEFAULT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `src_user` TEXT,
    `src_ip` VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_configinfotag_datagrouptenanttag` (`data_id`,`group_id`,`tenant_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `config_tags_relation` (
    `id` BIGINT NOT NULL,
    `tag_name` VARCHAR(128) NOT NULL,
    `tag_type` VARCHAR(64) DEFAULT NULL,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) NOT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `nid` BIGINT NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (`nid`),
    UNIQUE KEY `uk_configtagrelation_configidtag` (`id`,`tag_name`,`tag_type`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `group_capacity` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `group_id` VARCHAR(128) NOT NULL DEFAULT '',
    `quota` INT UNSIGNED NOT NULL DEFAULT 0,
    `usage` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_size` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_aggr_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_aggr_size` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_history_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `his_config_info` (
    `id` BIGINT UNSIGNED NOT NULL,
    `nid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `data_id` VARCHAR(255) NOT NULL,
    `group_id` VARCHAR(128) NOT NULL,
    `app_name` VARCHAR(128) DEFAULT NULL,
    `content` LONGTEXT NOT NULL,
    `md5` VARCHAR(32) DEFAULT NULL,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `src_user` TEXT,
    `src_ip` VARCHAR(50) DEFAULT NULL,
    `op_type` CHAR(10) DEFAULT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `encrypted_data_key` TEXT NOT NULL,
    PRIMARY KEY (`nid`),
    KEY `idx_gmt_create` (`gmt_create`),
    KEY `idx_gmt_modified` (`gmt_modified`),
    KEY `idx_did` (`data_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tenant_capacity` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `tenant_id` VARCHAR(128) NOT NULL DEFAULT '',
    `quota` INT UNSIGNED NOT NULL DEFAULT 0,
    `usage` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_size` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_aggr_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_aggr_size` INT UNSIGNED NOT NULL DEFAULT 0,
    `max_history_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `gmt_create` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tenant_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `kp` VARCHAR(128) NOT NULL,
    `tenant_id` VARCHAR(128) DEFAULT '',
    `tenant_name` VARCHAR(128) DEFAULT '',
    `tenant_desc` VARCHAR(256) DEFAULT NULL,
    `create_source` VARCHAR(32) DEFAULT NULL,
    `gmt_create` BIGINT NOT NULL,
    `gmt_modified` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_info_kptenantid` (`kp`,`tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `users` (
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(500) NOT NULL,
    `enabled` BOOLEAN NOT NULL,
    PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `permissions` (
    `role` VARCHAR(50) NOT NULL,
    `resource` VARCHAR(128) NOT NULL,
    `action` VARCHAR(8) NOT NULL,
    UNIQUE KEY `uk_role_permission` (`role`,`resource`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
EOF
    
    log_success "Nacos 数据库初始化完成"
}

# 插入测试数据
insert_test_data() {
    log_info "插入测试数据..."
    
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<'EOF'
-- 插入测试用户
INSERT INTO `users` (`username`, `nickname`, `email`, `phone`, `password`, `bio`, `gender`, `status`) VALUES
('admin', '管理员', 'admin@langtou.com', '13800138000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', 'Langtou 平台管理员', 1, 1),
('test_user', '测试用户', 'test@langtou.com', '13800138001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '这是一个测试账号', 2, 1),
('langtou_official', '榔头官方', 'official@langtou.com', '13800138002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', 'Langtou 官方账号', 1, 1);

-- 插入测试内容
INSERT INTO `contents` (`user_id`, `title`, `content`, `type`, `cover`, `tags`, `location`, `status`) VALUES
(3, '欢迎来到榔头社区', '榔头(Langtou)是一个新兴的社交内容社区,在这里你可以分享生活、发现美好。', 1, 'https://picsum.photos/800/600?random=1', '["欢迎", "社区", "介绍"]', '上海', 1),
(3, '如何拍出好看的照片', '分享一些摄影小技巧,帮助你拍出更好看的照片...', 1, 'https://picsum.photos/800/600?random=2', '["摄影", "教程", "技巧"]', '北京', 1),
(2, '我的第一次旅行', '记录我的第一次独自旅行,去了很多有趣的地方...', 1, 'https://picsum.photos/800/600?random=3', '["旅行", "日记", "生活"]', '杭州', 1);
EOF
    
    log_success "测试数据插入完成"
}

# 主函数
main() {
    log_info "========================================"
    log_info "Langtou 数据库初始化"
    log_info "========================================"
    
    check_mysql
    init_database
    init_tables
    init_nacos_db
    
    # 询问是否插入测试数据
    if [ "${INSERT_TEST_DATA:-false}" = "true" ]; then
        insert_test_data
    fi
    
    log_success "========================================"
    log_success "数据库初始化完成!"
    log_success "========================================"
    echo ""
    log_info "数据库信息:"
    echo "  - 主机: $DB_HOST:$DB_PORT"
    echo "  - 数据库: $DB_NAME"
    echo "  - 用户: $DB_USER"
    echo "  - Nacos数据库: nacos"
    echo ""
    log_info "如需插入测试数据,请设置环境变量: INSERT_TEST_DATA=true"
}

main "$@"
