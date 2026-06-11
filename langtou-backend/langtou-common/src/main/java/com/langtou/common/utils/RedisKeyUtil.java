package com.langtou.common.utils;

/**
 * Redis Key 工具类，统一管理缓存Key
 */
public class RedisKeyUtil {

    // ===== 内容服务缓存 =====

    /**
     * 笔记详情缓存: note:detail:{noteId}
     */
    public static String noteDetailKey(Long noteId) {
        return "note:detail:" + noteId;
    }

    /**
     * Feed列表缓存: feed:page:{page}:{size}
     */
    public static String feedPageKey(int page, int size) {
        return "feed:page:" + page + ":" + size;
    }

    /**
     * 关注Feed列表缓存: feed:following:{userId}:{page}:{size}
     */
    public static String followingFeedKey(Long userId, int page, int size) {
        return "feed:following:" + userId + ":" + page + ":" + size;
    }

    /**
     * 热门标签缓存: tags:hot:{limit}
     */
    public static String hotTagsKey(int limit) {
        return "tags:hot:" + limit;
    }

    // ===== 用户服务缓存 =====

    /**
     * 用户资料缓存: user:profile:{userId}
     */
    public static String userProfileKey(Long userId) {
        return "user:profile:" + userId;
    }

    /**
     * 粉丝列表缓存: user:followers:{userId}:{page}:{size}
     */
    public static String followersKey(Long userId, int page, int size) {
        return "user:followers:" + userId + ":" + page + ":" + size;
    }

    /**
     * 关注列表缓存: user:following:{userId}:{page}:{size}
     */
    public static String followingKey(Long userId, int page, int size) {
        return "user:following:" + userId + ":" + page + ":" + size;
    }

    // ===== 缓存TTL常量（秒） =====

    /** 笔记详情缓存TTL：30分钟 */
    public static final long NOTE_DETAIL_TTL = 30 * 60;

    /** Feed列表缓存TTL：5分钟 */
    public static final long FEED_PAGE_TTL = 5 * 60;

    /** 关注Feed列表缓存TTL：5分钟 */
    public static final long FOLLOWING_FEED_TTL = 5 * 60;

    /** 热门标签缓存TTL：1小时 */
    public static final long HOT_TAGS_TTL = 60 * 60;

    /** 用户资料缓存TTL：30分钟 */
    public static final long USER_PROFILE_TTL = 30 * 60;

    /** 关注/粉丝列表缓存TTL：10分钟 */
    public static final long FOLLOW_LIST_TTL = 10 * 60;
}
