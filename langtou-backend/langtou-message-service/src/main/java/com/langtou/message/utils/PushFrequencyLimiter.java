package com.langtou.message.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 推送频率限制器
 * 基于Redis滑动窗口实现每日推送频率控制
 *
 * Redis Key设计:
 *   push:freq:{userId}:{date} -> String (计数器)
 *   push:freq:marketing:{userId}:{date} -> String (营销推送计数器)
 *
 * TTL: 每日计数器在次日自动过期（48小时兜底）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushFrequencyLimiter {

    private final StringRedisTemplate redisTemplate;

    /** Redis Key前缀 - 每日总推送计数 */
    private static final String KEY_PREFIX = "push:freq:";

    /** Redis Key前缀 - 营销推送计数 */
    private static final String MARKETING_KEY_PREFIX = "push:freq:marketing:";

    /** 计数器TTL: 48小时（确保跨时区安全） */
    private static final long KEY_TTL_HOURS = 48;

    /** 每日营销推送上限 */
    private static final int DAILY_MARKETING_LIMIT = 2;

    /**
     * 尝试获取推送许可
     * 检查用户当日推送总数是否超过限制
     *
     * @param userId   用户ID
     * @param dailyLimit 每日推送上限
     * @return true=允许推送, false=超过频率限制
     */
    public boolean tryAcquire(Long userId, int dailyLimit) {
        String dateStr = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        String key = KEY_PREFIX + userId + ":" + dateStr;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次计数，设置TTL
            redisTemplate.expire(key, KEY_TTL_HOURS, TimeUnit.HOURS);
        }

        if (count != null && count > dailyLimit) {
            // 超过限制，回退计数
            redisTemplate.opsForValue().decrement(key);
            log.debug("推送频率限制触发: userId={}, count={}, limit={}", userId, count, dailyLimit);
            return false;
        }

        return true;
    }

    /**
     * 尝试获取营销推送许可
     * 营销推送有独立的每日上限（默认2条）
     *
     * @param userId 用户ID
     * @return true=允许推送, false=超过营销推送频率限制
     */
    public boolean tryAcquireMarketing(Long userId) {
        String dateStr = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        String key = MARKETING_KEY_PREFIX + userId + ":" + dateStr;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, KEY_TTL_HOURS, TimeUnit.HOURS);
        }

        if (count != null && count > DAILY_MARKETING_LIMIT) {
            redisTemplate.opsForValue().decrement(key);
            log.debug("营销推送频率限制触发: userId={}, count={}, limit={}", userId, count, DAILY_MARKETING_LIMIT);
            return false;
        }

        return true;
    }

    /**
     * 获取用户当日已推送次数
     *
     * @param userId 用户ID
     * @return 当日已推送次数
     */
    public long getTodayCount(Long userId) {
        String dateStr = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        String key = KEY_PREFIX + userId + ":" + dateStr;
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Long.parseLong(countStr) : 0;
    }

    /**
     * 获取用户当日营销推送次数
     *
     * @param userId 用户ID
     * @return 当日营销推送次数
     */
    public long getTodayMarketingCount(Long userId) {
        String dateStr = LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();
        String key = MARKETING_KEY_PREFIX + userId + ":" + dateStr;
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Long.parseLong(countStr) : 0;
    }
}
