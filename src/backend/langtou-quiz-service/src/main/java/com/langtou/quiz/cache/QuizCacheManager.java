package com.langtou.quiz.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.quiz.entity.QuizSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizCacheManager {

    private static final String QUIZ_SET_KEY_PREFIX = "quiz:set:";
    private static final String QUIZ_ATTEMPT_LOCK_KEY_PREFIX = "quiz:attempt:";
    private static final String LOCK_SUFFIX = ":lock";

    private static final Duration QUIZ_SET_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration SUBMIT_LOCK_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void cacheQuizSet(QuizSet quizSet) {
        if (quizSet == null || quizSet.getId() == null) {
            return;
        }
        try {
            String value = objectMapper.writeValueAsString(quizSet);
            String key = QUIZ_SET_KEY_PREFIX + quizSet.getId();
            stringRedisTemplate.opsForValue().set(key, value, QUIZ_SET_CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("QuizSet 缓存写入失败: id={}", quizSet.getId(), e);
        }
    }

    public QuizSet getQuizSet(Long quizSetId) {
        if (quizSetId == null) {
            return null;
        }
        String key = QUIZ_SET_KEY_PREFIX + quizSetId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, QuizSet.class);
        } catch (JsonProcessingException e) {
            log.warn("QuizSet 缓存读取失败: id={}", quizSetId, e);
            stringRedisTemplate.delete(key);
            return null;
        }
    }

    public void evictQuizSet(Long quizSetId) {
        if (quizSetId == null) {
            return;
        }
        stringRedisTemplate.delete(QUIZ_SET_KEY_PREFIX + quizSetId);
    }

    public boolean trySubmitLock(Long attemptId) {
        if (attemptId == null) {
            return false;
        }
        String key = QUIZ_ATTEMPT_LOCK_KEY_PREFIX + attemptId + LOCK_SUFFIX;
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", SUBMIT_LOCK_TTL.toSeconds(), TimeUnit.SECONDS);
        return Boolean.TRUE.equals(locked);
    }

    public void releaseSubmitLock(Long attemptId) {
        if (attemptId == null) {
            return;
        }
        stringRedisTemplate.delete(QUIZ_ATTEMPT_LOCK_KEY_PREFIX + attemptId + LOCK_SUFFIX);
    }
}
