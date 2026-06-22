package com.langtou.quiz.service.impl;

import com.langtou.quiz.dto.LeaderboardEntry;
import com.langtou.quiz.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final String QUIZ_KEY_PREFIX = "lb:quiz:";
    private static final String GLOBAL_KEY = "lb:quiz:global";
    private static final String FRIEND_KEY_PREFIX = "lb:quiz:friends:";
    private static final String USERNAME_KEY_PREFIX = "lb:quiz:user:name:";
    private static final String AVATAR_KEY_PREFIX = "lb:quiz:user:avatar:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void updateBestScore(Long userId, Long quizSetId, int score) {
        if (userId == null || quizSetId == null) {
            return;
        }
        String quizKey = QUIZ_KEY_PREFIX + quizSetId;
        String member = String.valueOf(userId);
        Double oldScore = stringRedisTemplate.opsForZSet().score(quizKey, member);
        if (oldScore == null || score > oldScore.intValue()) {
            stringRedisTemplate.opsForZSet().add(quizKey, member, score);
        }
        stringRedisTemplate.opsForZSet().add(GLOBAL_KEY, member, score);
    }

    @Override
    public List<LeaderboardEntry> getQuizLeaderboard(Long quizSetId, int limit) {
        String key = QUIZ_KEY_PREFIX + quizSetId;
        return fetchLeaderboard(key, limit, quizSetId);
    }

    @Override
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        return fetchLeaderboard(GLOBAL_KEY, limit, null);
    }

    @Override
    public List<LeaderboardEntry> getFriendLeaderboard(Long userId, int limit) {
        String key = FRIEND_KEY_PREFIX + userId;
        return fetchLeaderboard(key, limit, null);
    }

    private List<LeaderboardEntry> fetchLeaderboard(String key, int limit, Long quizSetId) {
        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<String>> tuples = zSetOps.reverseRangeWithScores(key, 0, limit - 1L);
        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }
        List<LeaderboardEntry> entries = new ArrayList<>(tuples.size());
        long rank = 1L;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setUserId(parseLong(tuple.getValue()));
            entry.setScore(tuple.getScore() == null ? 0 : tuple.getScore().intValue());
            entry.setRank(rank++);
            entry.setQuizSetId(quizSetId);
            entry.setUsername(stringRedisTemplate.opsForValue().get(USERNAME_KEY_PREFIX + tuple.getValue()));
            entry.setAvatarUrl(stringRedisTemplate.opsForValue().get(AVATAR_KEY_PREFIX + tuple.getValue()));
            entry.setUpdatedAt(Instant.now().toEpochMilli());
            entries.add(entry);
        }
        return entries;
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void syncToMysql() {
        log.info("开始将 Redis 排行榜数据同步到 MySQL");
        try {
            Set<String> keys = stringRedisTemplate.keys(QUIZ_KEY_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }
            for (String key : keys) {
                if (GLOBAL_KEY.equals(key)) {
                    continue;
                }
                Long quizSetId = parseLong(key.substring(QUIZ_KEY_PREFIX.length()));
                if (quizSetId == null) {
                    continue;
                }
                List<LeaderboardEntry> entries = fetchLeaderboard(key, Integer.MAX_VALUE, quizSetId);
                if (entries.isEmpty()) {
                    continue;
                }
                List<String> userIds = entries.stream()
                        .map(e -> e.getUserId() == null ? null : String.valueOf(e.getUserId()))
                        .collect(Collectors.toList());
                log.info("同步关卡排行榜: quizSetId={}, size={}", quizSetId, userIds.size());
            }
            List<LeaderboardEntry> global = fetchLeaderboard(GLOBAL_KEY, Integer.MAX_VALUE, null);
            log.info("同步总排行榜: size={}", global.size());
        } catch (Exception e) {
            log.error("Redis 排行榜同步 MySQL 异常", e);
        }
    }
}
