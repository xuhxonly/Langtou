package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.ResultCode;
import com.langtou.content.entity.SensitiveWord;
import com.langtou.content.mapper.SensitiveWordMapper;
import com.langtou.content.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 敏感词服务实现（MySQL + Redis 双层持久化）
 *
 * Redis Hash结构存储：
 *   key: sensitive:words:all
 *   field: 敏感词内容 (word)
 *   value: "1" (表示存在)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis Hash key: 存储所有启用的敏感词
     */
    private static final String REDIS_SENSITIVE_WORDS_KEY = "sensitive:words:all";

    /**
     * Redis 缓存TTL: 24小时（防止Redis数据丢失后长期无数据）
     */
    private static final long REDIS_SENSITIVE_WORDS_TTL = 24 * 60 * 60;

    /**
     * 服务启动时从MySQL加载所有启用敏感词到Redis
     */
    @PostConstruct
    @Override
    public void initSensitiveWordsToRedis() {
        try {
            List<String> enabledWords = sensitiveWordMapper.selectAllEnabledWords();
            if (enabledWords != null && !enabledWords.isEmpty()) {
                // 清除旧的缓存
                stringRedisTemplate.delete(REDIS_SENSITIVE_WORDS_KEY);
                // 批量写入Redis Hash
                Map<String, String> wordMap = new HashMap<>();
                for (String word : enabledWords) {
                    wordMap.put(word, "1");
                }
                stringRedisTemplate.opsForHash().putAll(REDIS_SENSITIVE_WORDS_KEY, wordMap);
                stringRedisTemplate.expire(REDIS_SENSITIVE_WORDS_KEY, REDIS_SENSITIVE_WORDS_TTL, TimeUnit.SECONDS);
                log.info("敏感词初始化完成，共加载 {} 个敏感词到Redis", enabledWords.size());
            } else {
                log.info("数据库中无启用的敏感词");
            }
        } catch (Exception e) {
            log.error("敏感词初始化失败: {}", e.getMessage());
        }
    }

    @Override
    public SensitiveWord addSensitiveWord(String word, String category, String source) {
        if (!StringUtils.hasText(word)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "敏感词内容不能为空");
        }

        String trimmedWord = word.trim();

        // 检查是否已存在
        SensitiveWord existing = sensitiveWordMapper.selectByWord(trimmedWord);
        if (existing != null) {
            // 已存在则重新启用
            if (!"ENABLED".equals(existing.getStatus())) {
                existing.setStatus("ENABLED");
                sensitiveWordMapper.updateById(existing);
                syncToRedis(trimmedWord);
                log.info("敏感词已存在，重新启用: word={}", trimmedWord);
            }
            return existing;
        }

        // 创建新敏感词
        SensitiveWord sensitiveWord = new SensitiveWord();
        sensitiveWord.setWord(trimmedWord);
        sensitiveWord.setCategory(StringUtils.hasText(category) ? category : "custom");
        sensitiveWord.setSource(StringUtils.hasText(source) ? source : "CUSTOM");
        sensitiveWord.setStatus("ENABLED");
        sensitiveWordMapper.insert(sensitiveWord);

        // 同步到Redis
        syncToRedis(trimmedWord);

        log.info("添加敏感词成功: word={}, category={}, source={}", trimmedWord, category, source);
        return sensitiveWord;
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "敏感词不存在");
        }

        sensitiveWordMapper.deleteById(id);

        // 从Redis中移除
        removeFromRedis(sensitiveWord.getWord());

        log.info("删除敏感词成功: id={}, word={}", id, sensitiveWord.getWord());
    }

    @Override
    public void updateSensitiveWordStatus(Long id, String status) {
        SensitiveWord sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "敏感词不存在");
        }

        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态值无效，仅支持 ENABLED/DISABLED");
        }

        sensitiveWord.setStatus(status);
        sensitiveWordMapper.updateById(sensitiveWord);

        // 同步Redis
        if ("ENABLED".equals(status)) {
            syncToRedis(sensitiveWord.getWord());
        } else {
            removeFromRedis(sensitiveWord.getWord());
        }

        log.info("更新敏感词状态: id={}, word={}, status={}", id, sensitiveWord.getWord(), status);
    }

    @Override
    public PageResult<SensitiveWord> getSensitiveWordPage(int page, int size, String word, String category, String source, String status) {
        Page<SensitiveWord> pageParam = new Page<>(page, size);
        IPage<SensitiveWord> result = sensitiveWordMapper.selectPageByCondition(pageParam, word, category, source, status);
        return PageResult.of(result);
    }

    @Override
    public boolean containsSensitiveWord(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        // 优先从Redis获取敏感词集合
        Set<String> redisWords = getSensitiveWordsFromRedis();
        if (redisWords != null && !redisWords.isEmpty()) {
            String normalizedText = text.toLowerCase().replaceAll("\\s+", "");
            for (String word : redisWords) {
                if (normalizedText.contains(word.toLowerCase())) {
                    log.debug("检测到敏感词(来自Redis): {}", word);
                    return true;
                }
            }
        }

        // Redis无数据时fallback到MySQL
        try {
            List<String> dbWords = sensitiveWordMapper.selectAllEnabledWords();
            if (dbWords != null && !dbWords.isEmpty()) {
                String normalizedText = text.toLowerCase().replaceAll("\\s+", "");
                for (String word : dbWords) {
                    if (normalizedText.contains(word.toLowerCase())) {
                        log.debug("检测到敏感词(来自MySQL): {}", word);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("从MySQL查询敏感词失败: {}", e.getMessage());
        }

        return false;
    }

    @Override
    public List<String> getAllEnabledWords() {
        // 优先从Redis获取
        Set<String> redisWords = getSensitiveWordsFromRedis();
        if (redisWords != null && !redisWords.isEmpty()) {
            return new ArrayList<>(redisWords);
        }

        // fallback到MySQL
        try {
            List<String> dbWords = sensitiveWordMapper.selectAllEnabledWords();
            if (dbWords != null && !dbWords.isEmpty()) {
                return dbWords;
            }
        } catch (Exception e) {
            log.error("从MySQL查询敏感词失败: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public List<SensitiveWord> getAllSensitiveWords() {
        return sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>().orderByDesc(SensitiveWord::getCreateTime));
    }

    /**
     * 从Redis获取所有敏感词
     */
    private Set<String> getSensitiveWordsFromRedis() {
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(REDIS_SENSITIVE_WORDS_KEY);
            if (entries != null && !entries.isEmpty()) {
                return entries.keySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("从Redis读取敏感词失败: {}", e.getMessage());
        }
        return Collections.emptySet();
    }

    /**
     * 同步单个敏感词到Redis
     */
    private void syncToRedis(String word) {
        try {
            stringRedisTemplate.opsForHash().put(REDIS_SENSITIVE_WORDS_KEY, word, "1");
            // 刷新TTL
            stringRedisTemplate.expire(REDIS_SENSITIVE_WORDS_KEY, REDIS_SENSITIVE_WORDS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("同步敏感词到Redis失败: word={}, error={}", word, e.getMessage());
        }
    }

    /**
     * 从Redis移除单个敏感词
     */
    private void removeFromRedis(String word) {
        try {
            stringRedisTemplate.opsForHash().delete(REDIS_SENSITIVE_WORDS_KEY, word);
        } catch (Exception e) {
            log.error("从Redis移除敏感词失败: word={}, error={}", word, e.getMessage());
        }
    }
}
