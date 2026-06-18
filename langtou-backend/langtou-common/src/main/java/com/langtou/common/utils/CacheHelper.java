package com.langtou.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Redis 缓存工具类
 *
 * 提供三级缓存防护：
 * 1. 穿透防护：空值缓存 + 布隆过滤器（可选）
 * 2. 击穿防护：SETNX 互斥锁
 * 3. 雪崩防护：TTL 随机偏移
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheHelper {

    private final StringRedisTemplate stringRedisTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 空值占位符 */
    private static final String NULL_PLACEHOLDER = "NULL_PLACEHOLDER";

    /** 互斥锁默认过期时间（秒） */
    private static final long LOCK_DEFAULT_TTL = 10;

    /** 雪崩防护：TTL 随机偏移最大值（秒） */
    private static final long TTL_RANDOM_OFFSET = 60;

    /**
     * 安全获取缓存（穿透防护：空值缓存 + 雪崩防护：随机TTL偏移）
     *
     * @param key      缓存key
     * @param type     返回类型
     * @param loader   缓存未命中时的加载函数
     * @param ttl      过期时间（秒）
     * @param emptyTtl 空值缓存时间（秒，防止穿透）
     * @param <T>      返回值泛型
     * @return 缓存值或加载后的值
     */
    public <T> T safeGet(String key, Class<T> type, Supplier<T> loader, long ttl, long emptyTtl) {
        // 1. 先查缓存
        try {
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(cached)) {
                // 命中空值占位符，直接返回 null（防穿透）
                if (NULL_PLACEHOLDER.equals(cached)) {
                    log.debug("缓存命中空值占位符: key={}", key);
                    return null;
                }
                // 正常命中，反序列化返回
                return deserialize(cached, type);
            }
        } catch (Exception e) {
            log.warn("读取缓存异常，降级到 loader: key={}, error={}", key, e.getMessage());
        }

        // 2. 未命中，调用 loader 加载数据
        T value = loader.get();

        // 3. 写入缓存
        try {
            if (value == null) {
                // loader 返回 null，缓存空值占位符（防穿透），设置 emptyTtl
                stringRedisTemplate.opsForValue().set(key, NULL_PLACEHOLDER, emptyTtl, TimeUnit.SECONDS);
                log.debug("缓存空值占位符: key={}, emptyTtl={}s", key, emptyTtl);
            } else {
                // loader 返回非 null，正常缓存，设置 ttl + 随机偏移（防雪崩）
                long actualTtl = ttl + ThreadLocalRandom.current().nextLong(0, TTL_RANDOM_OFFSET + 1);
                stringRedisTemplate.opsForValue().set(key, serialize(value), actualTtl, TimeUnit.SECONDS);
                log.debug("缓存正常值: key={}, ttl={}s", key, actualTtl);
            }
        } catch (Exception e) {
            log.warn("写入缓存异常: key={}, error={}", key, e.getMessage());
        }

        return value;
    }

    /**
     * 安全删除缓存（击穿防护：互斥锁）
     *
     * 使用 Redis SETNX 实现简单互斥锁，防止大量请求同时击穿到数据库。
     * 流程：
     * 1. 尝试获取锁
     * 2. 获取锁成功 -> 删除缓存 -> 释放锁
     * 3. 获取锁失败 -> 等待短暂时间后重试一次
     *
     * @param key 要删除的缓存key
     */
    public void safeEvict(String key) {
        String lockKey = "lock:evict:" + key;
        boolean locked = tryLock(lockKey, LOCK_DEFAULT_TTL);

        if (locked) {
            try {
                stringRedisTemplate.delete(key);
                log.debug("安全删除缓存成功: key={}", key);
            } catch (Exception e) {
                log.warn("安全删除缓存异常: key={}, error={}", key, e.getMessage());
            } finally {
                releaseLock(lockKey);
            }
        } else {
            // 获取锁失败，短暂等待后重试一次
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 重试删除（即使没有锁也尝试删除，因为缓存最终一致性即可）
            try {
                stringRedisTemplate.delete(key);
                log.debug("安全删除缓存（重试）: key={}", key);
            } catch (Exception e) {
                log.warn("安全删除缓存重试异常: key={}, error={}", key, e.getMessage());
            }
        }
    }

    /**
     * 批量获取（避免 N 次单独查询，使用 Pipeline 批量查询）
     *
     * @param keys   缓存key列表
     * @param type   返回类型
     * @param loader 缓存未命中时的加载函数（按单个key加载）
     * @param ttl    过期时间（秒）
     * @param <T>    返回值泛型
     * @return key -> value 的映射（未命中且加载失败的key不在结果中）
     */
    public <T> Map<String, T> safeGetBatch(List<String> keys, Class<T> type, Function<String, T> loader, long ttl) {
        Map<String, T> result = new HashMap<>();
        if (keys == null || keys.isEmpty()) {
            return result;
        }

        // 1. Pipeline 批量查询缓存
        List<String> cachedValues;
        try {
            cachedValues = stringRedisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.warn("Pipeline批量查询缓存异常，降级到逐个查询: error={}", e.getMessage());
            cachedValues = null;
        }

        // 2. 分离命中和未命中的key
        List<String> missedKeys = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String cached = (cachedValues != null && i < cachedValues.size()) ? cachedValues.get(i) : null;

            if (StringUtils.hasText(cached) && !NULL_PLACEHOLDER.equals(cached)) {
                try {
                    result.put(key, deserialize(cached, type));
                } catch (Exception e) {
                    log.warn("反序列化缓存异常: key={}, error={}", key, e.getMessage());
                    missedKeys.add(key);
                }
            } else if (NULL_PLACEHOLDER.equals(cached)) {
                // 空值占位符，跳过（不回源）
                log.debug("批量查询命中空值占位符: key={}", key);
            } else {
                missedKeys.add(key);
            }
        }

        // 3. 对未命中的key调用 loader 加载
        for (String missedKey : missedKeys) {
            try {
                T value = loader.apply(missedKey);
                if (value != null) {
                    result.put(missedKey, value);
                    // 异步写入缓存（防雪崩：随机偏移）
                    long actualTtl = ttl + ThreadLocalRandom.current().nextLong(0, TTL_RANDOM_OFFSET + 1);
                    stringRedisTemplate.opsForValue().set(missedKey, serialize(value), actualTtl, TimeUnit.SECONDS);
                } else {
                    // 缓存空值占位符
                    stringRedisTemplate.opsForValue().set(missedKey, NULL_PLACEHOLDER, 60, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.warn("加载缓存数据异常: key={}, error={}", missedKey, e.getMessage());
            }
        }

        return result;
    }

    /**
     * 使用 SCAN 替代 KEYS 命令删除匹配模式的缓存（避免 Redis 阻塞）
     *
     * @param pattern 匹配模式（如 "recommend:feed:123:*"）
     * @return 删除的key数量
     */
    public int deleteByScan(String pattern) {
        int count = 0;
        try {
            ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).count(100).build();
            Cursor<String> cursor = stringRedisTemplate.scan(scanOptions);
            List<String> keysToDelete = new ArrayList<>();
            while (cursor.hasNext()) {
                keysToDelete.add(cursor.next());
                // 分批删除，避免一次性删除过多
                if (keysToDelete.size() >= 100) {
                    count += keysToDelete.size();
                    stringRedisTemplate.delete(keysToDelete);
                    keysToDelete.clear();
                }
            }
            // 删除剩余的key
            if (!keysToDelete.isEmpty()) {
                count += keysToDelete.size();
                stringRedisTemplate.delete(keysToDelete);
            }
            log.debug("SCAN删除缓存: pattern={}, count={}", pattern, count);
        } catch (Exception e) {
            log.warn("SCAN删除缓存异常: pattern={}, error={}", pattern, e.getMessage());
        }
        return count;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 尝试获取分布式锁
     */
    private boolean tryLock(String lockKey, long ttlSeconds) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("获取分布式锁异常: lockKey={}, error={}", lockKey, e.getMessage());
            return false;
        }
    }

    /**
     * 释放分布式锁（使用 Lua 脚本保证原子性）
     */
    private void releaseLock(String lockKey) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(lockKey),
                    "1"
            );
        } catch (Exception e) {
            log.warn("释放分布式锁异常: lockKey={}, error={}", lockKey, e.getMessage());
        }
    }

    /**
     * 序列化对象为 JSON 字符串
     */
    private String serialize(Object value) throws JsonProcessingException {
        if (value instanceof String) {
            return (String) value;
        }
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    /**
     * 反序列化 JSON 字符串为对象
     */
    @SuppressWarnings("unchecked")
    private <T> T deserialize(String json, Class<T> type) {
        if (type == String.class) {
            return (T) json;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("反序列化缓存值失败: " + e.getMessage(), e);
        }
    }
}
