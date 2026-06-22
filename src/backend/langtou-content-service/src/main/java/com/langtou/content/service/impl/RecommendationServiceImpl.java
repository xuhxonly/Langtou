package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.langtou.common.client.UserClient;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.utils.CacheHelper;
import com.langtou.common.utils.RedisKeyUtil;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.entity.Content;
import com.langtou.content.entity.UserProfile;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.mapper.UserProfileMapper;
import com.langtou.content.service.RecommendationService;
import com.langtou.content.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务实现类
 *
 * 架构分层：
 * 1. 召回层（5通道）：协同过滤 / 内容相似 / 热门 / 关注 / 地域
 * 2. 排序层：加权排序（后续接入 DeepFM/DIN 模型）
 * 3. 冷启动：新用户返回热门 + 编辑精选
 * 4. 实时反馈：行为写入 Redis，定时刷新画像
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final ContentMapper contentMapper;
    private final UserProfileMapper userProfileMapper;
    private final TagService tagService;
    private final UserClient userClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheHelper cacheHelper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Caffeine 本地缓存：用户信息缓存 TTL 5min，最大容量 10000 */
    private final Cache<Long, Map<String, Object>> userLocalCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    // ===== 行为权重常量 =====
    private static final double ACTION_WEIGHT_VIEW = 1.0;
    private static final double ACTION_WEIGHT_LIKE = 3.0;
    private static final double ACTION_WEIGHT_COMMENT = 4.0;
    private static final double ACTION_WEIGHT_COLLECT = 5.0;
    private static final double ACTION_WEIGHT_SHARE = 6.0;

    // ===== 召回通道数量配置 =====
    private static final int RECALL_CHANNEL_SIZE = 50;

    // ===== 排序权重配置（后续由 DeepFM 替代） =====
    private static final double SCORE_USER_INTEREST = 0.35;
    private static final double SCORE_HOT_TREND = 0.25;
    private static final double SCORE_FOLLOWING = 0.20;
    private static final double SCORE_GEO_PROXIMITY = 0.10;
    private static final double SCORE_TIME_DECAY = 0.10;

    // ===== 冷启动保底曝光配置 =====
    private static final int COLD_START_HOT_RATIO = 60;
    private static final int COLD_START_EDITOR_RATIO = 40;

    @Override
    public List<NoteFeedVO> recommendFeed(Long userId, int page, int size) {
        if (userId == null) {
            return recommendForNewUser(page, size);
        }

        // 1. 检查用户画像是否存在
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null || CollectionUtils.isEmpty(profile.getInterestTags())) {
            log.info("用户画像为空，使用冷启动推荐: userId={}", userId);
            return recommendForNewUser(page, size);
        }

        // 2. 使用 CacheHelper 安全获取缓存（穿透/雪崩防护）
        String cacheKey = RedisKeyUtil.userRecommendKey(userId, page, size);
        try {
            List<NoteFeedVO> cached = cacheHelper.safeGet(
                    cacheKey,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NoteFeedVO.class),
                    () -> null, // 占位 loader，实际加载在下方
                    RedisKeyUtil.RECOMMEND_FEED_TTL,
                    60 // 空值缓存60秒
            );
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取推荐缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        // 3. 召回层：5通道召回
        Set<Long> candidateIds = recallLayer(userId, profile);

        // 4. 排序层：加权排序
        List<Content> sortedContents = rankLayer(userId, profile, new ArrayList<>(candidateIds), size);

        // 5. 转换为 VO 并填充作者信息
        List<NoteFeedVO> result = sortedContents.stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());
        fillFeedAuthorInfoBatch(result);

        // 6. 写入 Redis 缓存（使用 CacheHelper，自动添加随机 TTL 偏移防雪崩）
        try {
            long actualTtl = RedisKeyUtil.RECOMMEND_FEED_TTL + ThreadLocalRandom.current().nextLong(0, 61);
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(actualTtl));
        } catch (Exception e) {
            log.warn("写入推荐缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return result;
    }

    @Override
    public List<NoteFeedVO> recommendForNewUser(int page, int size) {
        // 冷启动策略：热门内容 + 编辑精选，保底曝光
        int hotSize = size * COLD_START_HOT_RATIO / 100;
        int editorSize = size * COLD_START_EDITOR_RATIO / 100;

        // 热门召回
        List<Content> hotContents = recallHotNotes(hotSize);

        // 编辑精选召回（status = 1 且 view_count 较高的内容作为精选）
        List<Content> editorContents = recallEditorPicks(editorSize);

        // 合并去重
        Set<Long> seenIds = new LinkedHashSet<>();
        List<Content> merged = new ArrayList<>();
        for (Content c : hotContents) {
            if (seenIds.add(c.getId())) {
                merged.add(c);
            }
        }
        for (Content c : editorContents) {
            if (seenIds.add(c.getId())) {
                merged.add(c);
            }
        }

        // 按热度排序
        merged.sort((a, b) -> {
            double scoreA = calculateHotScore(a);
            double scoreB = calculateHotScore(b);
            return Double.compare(scoreB, scoreA);
        });

        List<NoteFeedVO> result = merged.stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());
        fillFeedAuthorInfoBatch(result);

        return result;
    }

    @Override
    public void recordUserAction(Long userId, Long noteId, String actionType) {
        if (userId == null || noteId == null || !StringUtils.hasText(actionType)) {
            return;
        }

        double weight = getActionWeight(actionType);
        String actionKey = "user:action:" + userId;
        String noteField = noteId.toString();

        // 记录行为到 Redis Hash（用于实时画像更新）
        stringRedisTemplate.opsForHash().increment(actionKey, noteField, weight);
        stringRedisTemplate.expire(actionKey, Duration.ofMinutes(30));

        // 记录行为时间线（用于实时反馈，5分钟内影响推荐）
        String timelineKey = "user:timeline:" + userId;
        long timestamp = System.currentTimeMillis();
        stringRedisTemplate.opsForZSet().add(timelineKey, noteId + ":" + actionType, timestamp);
        stringRedisTemplate.expire(timelineKey, Duration.ofMinutes(30));

        // 记录笔记被互动统计（用于热门排序）
        String noteInteractKey = "note:interact:" + noteId;
        stringRedisTemplate.opsForHash().increment(noteInteractKey, actionType, 1);
        stringRedisTemplate.expire(noteInteractKey, Duration.ofHours(24));

        // 异步触发画像刷新（简化版：直接调用，生产环境建议用 MQ）
        if (weight >= ACTION_WEIGHT_LIKE) {
            try {
                refreshUserProfile(userId);
            } catch (Exception e) {
                log.warn("刷新用户画像失败: userId={}, error={}", userId, e.getMessage());
            }
        }

        log.info("记录用户行为: userId={}, noteId={}, actionType={}, weight={}", userId, noteId, actionType, weight);
    }

    @Override
    public void refreshUserProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setInterestTags(new HashMap<>());
            profile.setBehaviorFeatures(new HashMap<>());
            profile.setDemographicFeatures(new HashMap<>());
        }

        // 从 Redis 读取近期行为
        String actionKey = "user:action:" + userId;
        Map<Object, Object> actions = stringRedisTemplate.opsForHash().entries(actionKey);
        if (CollectionUtils.isEmpty(actions)) {
            return;
        }

        // 计算兴趣标签权重（批量查询标签，避免N+1）
        List<Long> noteIds = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : actions.entrySet()) {
            noteIds.add(Long.valueOf(entry.getKey().toString()));
        }

        Map<String, Double> tagWeights = new HashMap<>();
        Map<Long, List<com.langtou.content.entity.Tag>> noteTagsMap = tagService.getTagsByNoteIds(noteIds);
        for (Map.Entry<Object, Object> entry : actions.entrySet()) {
            Long noteId = Long.valueOf(entry.getKey().toString());
            Double weight = Double.valueOf(entry.getValue().toString());

            List<com.langtou.content.entity.Tag> tags = noteTagsMap.getOrDefault(noteId, Collections.emptyList());
            for (com.langtou.content.entity.Tag tag : tags) {
                tagWeights.merge(tag.getName(), weight, Double::sum);
            }
        }

        // 归一化权重
        double maxWeight = tagWeights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        Map<String, Double> normalizedTags = new HashMap<>();
        for (Map.Entry<String, Double> entry : tagWeights.entrySet()) {
            normalizedTags.put(entry.getKey(), Math.min(entry.getValue() / maxWeight, 1.0));
        }

        profile.setInterestTags(normalizedTags);
        profile.setLastActiveAt(LocalDateTime.now());

        // 更新行为特征
        Map<String, Object> behavior = profile.getBehaviorFeatures() != null ? profile.getBehaviorFeatures() : new HashMap<>();
        behavior.put("totalActions", actions.size());
        behavior.put("lastRefreshTime", System.currentTimeMillis());
        profile.setBehaviorFeatures(behavior);

        // 保存画像
        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        // 清除推荐缓存，确保实时反馈生效（使用 SCAN 替代 KEYS，避免 Redis 阻塞）
        String pattern = "recommend:feed:" + userId + ":*";
        cacheHelper.deleteByScan(pattern);

        log.info("用户画像刷新成功: userId={}, tags={}", userId, normalizedTags.keySet());
    }

    // ==================== 召回层：5通道 ====================

    /**
     * 5通道召回
     */
    private Set<Long> recallLayer(Long userId, UserProfile profile) {
        Set<Long> candidates = new LinkedHashSet<>();

        // 通道1：协同过滤召回（基于相似用户）
        candidates.addAll(recallCollaborativeFiltering(userId));

        // 通道2：内容相似召回（基于兴趣标签）
        candidates.addAll(recallContentSimilarity(profile));

        // 通道3：热门趋势召回
        candidates.addAll(recallHotTrends());

        // 通道4：关注召回
        candidates.addAll(recallFollowing(userId));

        // 通道5：地域召回
        candidates.addAll(recallGeoProximity(profile));

        return candidates;
    }

    /**
     * 通道1：协同过滤召回
     * 基于用户历史互动笔记，召回相似用户互动的笔记
     */
    private Set<Long> recallCollaborativeFiltering(Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        try {
            // 获取用户近期互动笔记
            String actionKey = "user:action:" + userId;
            Set<Object> noteIds = stringRedisTemplate.opsForHash().keys(actionKey);
            if (CollectionUtils.isEmpty(noteIds)) {
                return result;
            }

            // 简化版协同过滤：基于标签相似度召回（批量查询标签，避免N+1）
            List<Long> noteIdList = noteIds.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
            Map<Long, List<com.langtou.content.entity.Tag>> noteTagsMap = tagService.getTagsByNoteIds(noteIdList);

            Set<String> userTags = new HashSet<>();
            for (Map.Entry<Long, List<com.langtou.content.entity.Tag>> entry : noteTagsMap.entrySet()) {
                for (com.langtou.content.entity.Tag tag : entry.getValue()) {
                    userTags.add(tag.getName());
                }
            }

            // 查询包含这些标签的热门笔记
            if (!userTags.isEmpty()) {
                QueryWrapper<Content> wrapper = new QueryWrapper<>();
                wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                        .orderByDesc("like_count", "comment_count", "view_count")
                        .last("LIMIT " + RECALL_CHANNEL_SIZE);
                List<Content> contents = contentMapper.selectList(wrapper);
                for (Content c : contents) {
                    result.add(c.getId());
                }
            }
        } catch (Exception e) {
            log.warn("协同过滤召回失败: userId={}, error={}", userId, e.getMessage());
        }
        return result;
    }

    /**
     * 通道2：内容相似召回
     * 基于用户兴趣标签召回相关内容
     */
    private Set<Long> recallContentSimilarity(UserProfile profile) {
        Set<Long> result = new LinkedHashSet<>();
        if (profile == null || CollectionUtils.isEmpty(profile.getInterestTags())) {
            return result;
        }

        try {
            // 取权重 Top N 的标签
            List<String> topTags = profile.getInterestTags().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String tagName : topTags) {
                com.langtou.content.entity.Tag tag = tagService.getOrCreateTag(tagName);
                List<Long> noteIds = tagService.getNoteIdsByTagId(tag.getId());
                if (!CollectionUtils.isEmpty(noteIds)) {
                    // 取每个标签下最新的笔记
                    List<Long> limited = noteIds.stream().limit(10).collect(Collectors.toList());
                    result.addAll(limited);
                }
                if (result.size() >= RECALL_CHANNEL_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("内容相似召回失败: error={}", e.getMessage());
        }
        return result;
    }

    /**
     * 通道3：热门趋势召回
     */
    private Set<Long> recallHotTrends() {
        Set<Long> result = new LinkedHashSet<>();
        try {
            QueryWrapper<Content> wrapper = new QueryWrapper<>();
            wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                    .ge("created_at", LocalDateTime.now().minusDays(7))
                    .orderByDesc("view_count", "like_count", "comment_count")
                    .last("LIMIT " + RECALL_CHANNEL_SIZE);
            List<Content> contents = contentMapper.selectList(wrapper);
            for (Content c : contents) {
                result.add(c.getId());
            }
        } catch (Exception e) {
            log.warn("热门趋势召回失败: error={}", e.getMessage());
        }
        return result;
    }

    /**
     * 通道4：关注召回
     */
    private Set<Long> recallFollowing(Long userId) {
        Set<Long> result = new LinkedHashSet<>();
        try {
            Result<List<Long>> followingResult = userClient.getFollowingIds(userId);
            if (followingResult == null || CollectionUtils.isEmpty(followingResult.getData())) {
                return result;
            }

            List<Long> followingIds = followingResult.getData();
            QueryWrapper<Content> wrapper = new QueryWrapper<>();
            wrapper.in("user_id", followingIds)
                    .eq("status", CommonConstants.STATUS_ENABLE)
                    .orderByDesc("created_at")
                    .last("LIMIT " + RECALL_CHANNEL_SIZE);
            List<Content> contents = contentMapper.selectList(wrapper);
            for (Content c : contents) {
                result.add(c.getId());
            }
        } catch (Exception e) {
            log.warn("关注召回失败: userId={}, error={}", userId, e.getMessage());
        }
        return result;
    }

    /**
     * 通道5：地域召回
     */
    private Set<Long> recallGeoProximity(UserProfile profile) {
        Set<Long> result = new LinkedHashSet<>();
        if (profile == null || profile.getDemographicFeatures() == null) {
            return result;
        }

        Object regionObj = profile.getDemographicFeatures().get("region");
        if (regionObj == null) {
            return result;
        }

        String region = regionObj.toString();
        try {
            QueryWrapper<Content> wrapper = new QueryWrapper<>();
            wrapper.like("location", region)
                    .eq("status", CommonConstants.STATUS_ENABLE)
                    .orderByDesc("created_at")
                    .last("LIMIT " + RECALL_CHANNEL_SIZE);
            List<Content> contents = contentMapper.selectList(wrapper);
            for (Content c : contents) {
                result.add(c.getId());
            }
        } catch (Exception e) {
            log.warn("地域召回失败: region={}, error={}", region, e.getMessage());
        }
        return result;
    }

    // ==================== 排序层 ====================

    /**
     * 排序层：加权排序（后续接入 DeepFM/DIN）
     */
    private List<Content> rankLayer(Long userId, UserProfile profile, List<Long> candidateIds, int size) {
        if (CollectionUtils.isEmpty(candidateIds)) {
            return Collections.emptyList();
        }

        // 批量查询内容
        List<Content> contents = contentMapper.selectBatchIds(candidateIds);
        contents = contents.stream()
                .filter(c -> CommonConstants.STATUS_ENABLE.equals(c.getStatus()))
                .collect(Collectors.toList());

        // 计算每篇笔记的排序分
        Map<Long, Double> scores = new HashMap<>();
        for (Content content : contents) {
            double score = calculateRankScore(userId, profile, content);
            scores.put(content.getId(), score);
        }

        // 按分数降序排列
        contents.sort((a, b) -> Double.compare(scores.getOrDefault(b.getId(), 0.0),
                scores.getOrDefault(a.getId(), 0.0)));

        // 返回 Top N
        return contents.stream().limit(size).collect(Collectors.toList());
    }

    /**
     * 计算单篇笔记的排序分数
     */
    private double calculateRankScore(Long userId, UserProfile profile, Content content) {
        double score = 0.0;

        // 1. 用户兴趣匹配度
        score += SCORE_USER_INTEREST * calculateInterestScore(profile, content);

        // 2. 热度趋势分
        score += SCORE_HOT_TREND * calculateHotScore(content);

        // 3. 关注作者加分
        score += SCORE_FOLLOWING * calculateFollowingScore(userId, content);

        // 4. 地域 proximity 分
        score += SCORE_GEO_PROXIMITY * calculateGeoScore(profile, content);

        // 5. 时间衰减分
        score += SCORE_TIME_DECAY * calculateTimeDecayScore(content);

        return score;
    }

    private double calculateInterestScore(UserProfile profile, Content content) {
        if (profile == null || CollectionUtils.isEmpty(profile.getInterestTags())) {
            return 0.0;
        }
        List<com.langtou.content.entity.Tag> tags = tagService.getTagsByNoteId(content.getId());
        if (CollectionUtils.isEmpty(tags)) {
            return 0.0;
        }
        double score = 0.0;
        for (com.langtou.content.entity.Tag tag : tags) {
            Double weight = profile.getInterestTags().get(tag.getName());
            if (weight != null) {
                score += weight;
            }
        }
        return Math.min(score, 1.0);
    }

    private double calculateHotScore(Content content) {
        // 简化版热度分：综合互动量
        int interactions = content.getLikeCount() + content.getCommentCount() * 2
                + content.getCollectCount() * 3 + content.getShareCount() * 4;
        // 使用 sigmoid 归一化
        return 1.0 / (1.0 + Math.exp(-interactions / 100.0));
    }

    private double calculateFollowingScore(Long userId, Content content) {
        try {
            Result<List<Long>> result = userClient.getFollowingIds(userId);
            if (result != null && result.getData() != null) {
                return result.getData().contains(content.getUserId()) ? 1.0 : 0.0;
            }
        } catch (Exception e) {
            log.warn("计算关注分数失败: userId={}, error={}", userId, e.getMessage());
        }
        return 0.0;
    }

    private double calculateGeoScore(UserProfile profile, Content content) {
        if (profile == null || profile.getDemographicFeatures() == null
                || content.getLocation() == null) {
            return 0.0;
        }
        Object regionObj = profile.getDemographicFeatures().get("region");
        if (regionObj == null) {
            return 0.0;
        }
        return content.getLocation().contains(regionObj.toString()) ? 1.0 : 0.0;
    }

    private double calculateTimeDecayScore(Content content) {
        if (content.getCreatedAt() == null) {
            return 0.0;
        }
        long hoursAgo = java.time.Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours();
        // 指数衰减，72小时后接近0
        return Math.exp(-hoursAgo / 24.0);
    }

    // ==================== 冷启动辅助方法 ====================

    private List<Content> recallHotNotes(int size) {
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("view_count", "like_count", "comment_count")
                .last("LIMIT " + size);
        return contentMapper.selectList(wrapper);
    }

    private List<Content> recallEditorPicks(int size) {
        // 编辑精选策略：近7天高互动且内容质量好的笔记
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                .ge("created_at", LocalDateTime.now().minusDays(7))
                .ge("like_count", 10)
                .orderByDesc("like_count", "comment_count")
                .last("LIMIT " + size);
        return contentMapper.selectList(wrapper);
    }

    // ==================== 工具方法 ====================

    private double getActionWeight(String actionType) {
        return switch (actionType.toLowerCase()) {
            case "view" -> ACTION_WEIGHT_VIEW;
            case "like" -> ACTION_WEIGHT_LIKE;
            case "comment" -> ACTION_WEIGHT_COMMENT;
            case "collect" -> ACTION_WEIGHT_COLLECT;
            case "share" -> ACTION_WEIGHT_SHARE;
            default -> ACTION_WEIGHT_VIEW;
        };
    }

    private NoteFeedVO convertToFeedVO(Content content) {
        NoteFeedVO vo = new NoteFeedVO();
        BeanUtils.copyProperties(content, vo);
        vo.setId(content.getId());
        vo.setUserId(content.getUserId());
        vo.setTitle(content.getTitle());
        vo.setViewCount(content.getViewCount());
        vo.setLikeCount(content.getLikeCount());
        vo.setCommentCount(content.getCommentCount());
        vo.setCollectCount(content.getCollectCount());
        vo.setCreateTime(content.getCreatedAt());

        if (StringUtils.hasText(content.getContent())) {
            String text = content.getContent();
            vo.setSummary(text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }

        if (!CollectionUtils.isEmpty(content.getImages())) {
            vo.setCoverImage(content.getImages().get(0));
        }
        if (StringUtils.hasText(content.getVideoUrl())) {
            vo.setCoverImage(content.getVideoUrl());
        }

        // 标签
        List<com.langtou.content.entity.Tag> tags = tagService.getTagsByNoteId(content.getId());
        if (!CollectionUtils.isEmpty(tags)) {
            vo.setTags(tags.stream().map(com.langtou.content.entity.Tag::getName).collect(Collectors.toList()));
        }

        return vo;
    }

    /**
     * 批量填充FeedVO的作者信息，避免N+1问题
     * 使用 Caffeine 本地缓存减少 RPC 调用
     */
    private void fillFeedAuthorInfoBatch(List<NoteFeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> userIds = records.stream()
                .map(NoteFeedVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }

        // 先从 Caffeine 本地缓存获取
        Map<Long, Map<String, Object>> userMap = new HashMap<>();
        List<Long> uncachedUserIds = new ArrayList<>();
        for (Long userId : userIds) {
            Map<String, Object> cachedUser = userLocalCache.getIfPresent(userId);
            if (cachedUser != null) {
                userMap.put(userId, cachedUser);
            } else {
                uncachedUserIds.add(userId);
            }
        }

        // 本地缓存未命中的用户，通过 RPC 批量获取
        if (!uncachedUserIds.isEmpty()) {
            try {
                Result<List<Map<String, Object>>> result = userClient.batchGetUsers(uncachedUserIds);
                if (result != null && result.getData() != null) {
                    for (Map<String, Object> user : result.getData()) {
                        Object id = user.get("id");
                        if (id != null) {
                            Long userId = Long.valueOf(id.toString());
                            userMap.put(userId, user);
                            // 写入 Caffeine 本地缓存
                            userLocalCache.put(userId, user);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("批量获取用户信息失败, userIds={}, error={}", uncachedUserIds, e.getMessage());
            }
        }

        for (NoteFeedVO vo : records) {
            Long userId = vo.getUserId();
            Map<String, Object> user = userMap.get(userId);
            if (user != null) {
                vo.setAuthorNickname(user.get("nickname") != null ? user.get("nickname").toString() : "用户" + userId);
                vo.setAuthorAvatar(user.get("avatar") != null ? user.get("avatar").toString() : CommonConstants.DEFAULT_AVATAR);
            } else {
                vo.setAuthorNickname("用户" + userId);
                vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
            }
        }
    }
}
