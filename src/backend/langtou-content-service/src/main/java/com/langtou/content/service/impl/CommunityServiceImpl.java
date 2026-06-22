package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.content.entity.UserLevel;
import com.langtou.content.entity.UserAchievement;
import com.langtou.content.entity.OnboardingStep;
import com.langtou.content.entity.UserOnboardingProgress;
import com.langtou.content.mapper.UserLevelMapper;
import com.langtou.content.mapper.UserAchievementMapper;
import com.langtou.content.mapper.OnboardingStepMapper;
import com.langtou.content.mapper.UserOnboardingProgressMapper;
import com.langtou.content.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final UserLevelMapper userLevelMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final OnboardingStepMapper onboardingStepMapper;
    private final UserOnboardingProgressMapper userOnboardingProgressMapper;

    @Override
    public Map<String, Object> getUserLevel(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取用户积分（通过积分记录表计算）
        int userPoints = 0;
        try {
            // 假设用户积分存储在 user 表的 points 字段中
            // 这里使用 JdbcTemplate 查询，或者通过 Feign 调用用户服务
            // 简化处理：从 user_achievements 数量推算积分
            long achievementCount = userAchievementMapper.selectCount(
                    new LambdaQueryWrapper<UserAchievement>().eq(UserAchievement::getUserId, userId));
            userPoints = (int) (achievementCount * 100); // 简化积分计算
        } catch (Exception e) {
            log.warn("获取用户积分失败, userId={}, error={}", userId, e.getMessage());
        }

        // 查找匹配的等级
        List<UserLevel> allLevels = userLevelMapper.selectList(
                new LambdaQueryWrapper<UserLevel>().orderByAsc(UserLevel::getLevel));

        UserLevel currentLevel = null;
        UserLevel nextLevel = null;

        for (UserLevel level : allLevels) {
            if (userPoints >= level.getMinPoints() && userPoints <= level.getMaxPoints()) {
                currentLevel = level;
            }
        }

        if (currentLevel == null && !allLevels.isEmpty()) {
            // 超过最高等级
            currentLevel = allLevels.get(allLevels.size() - 1);
        }

        // 查找下一等级
        if (currentLevel != null) {
            for (UserLevel level : allLevels) {
                if (level.getLevel() > currentLevel.getLevel()) {
                    nextLevel = level;
                    break;
                }
            }
        }

        result.put("userId", userId);
        result.put("points", userPoints);
        result.put("currentLevel", currentLevel);
        result.put("nextLevel", nextLevel);
        if (nextLevel != null) {
            result.put("pointsToNextLevel", Math.max(0, nextLevel.getMinPoints() - userPoints));
        }

        return result;
    }

    @Override
    public List<UserLevel> getAllLevels() {
        return userLevelMapper.selectList(
                new LambdaQueryWrapper<UserLevel>().orderByAsc(UserLevel::getLevel));
    }

    @Override
    public List<UserAchievement> checkAndUnlockAchievements(Long userId, String eventType) {
        List<UserAchievement> newlyUnlocked = new ArrayList<>();

        // 根据事件类型检查可解锁的成就
        switch (eventType != null ? eventType.toUpperCase() : "") {
            case "FIRST_NOTE":
                newlyUnlocked.add(unlockAchievementIfNotExists(userId,
                        "FIRST_NOTE", "发布第一篇笔记", "恭喜你发布了第一篇笔记！"));
                break;

            case "FIRST_COMMENT":
                newlyUnlocked.add(unlockAchievementIfNotExists(userId,
                        "FIRST_COMMENT", "发表第一条评论", "恭喜你发表了第一条评论！"));
                break;

            case "LIKE_RECEIVED":
                // 检查点赞里程碑
                newlyUnlocked.addAll(checkLikeMilestones(userId));
                break;

            case "FOLLOWER_CHANGE":
                // 检查粉丝里程碑
                newlyUnlocked.addAll(checkFollowerMilestones(userId));
                break;

            case "LOGIN":
                // 检查连续登录
                newlyUnlocked.addAll(checkContinuousLogin(userId));
                break;

            case "PUBLISH_NOTE":
                // 检查内容里程碑
                newlyUnlocked.addAll(checkContentMilestones(userId));
                break;

            default:
                break;
        }

        // 过滤掉未实际解锁的(null)
        return newlyUnlocked.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private UserAchievement unlockAchievementIfNotExists(Long userId, String type, String name, String description) {
        // 检查是否已存在
        Long count = userAchievementMapper.selectCount(
                new LambdaQueryWrapper<UserAchievement>()
                        .eq(UserAchievement::getUserId, userId)
                        .eq(UserAchievement::getAchievementType, type));

        if (count > 0) {
            return null; // 已解锁
        }

        UserAchievement achievement = new UserAchievement();
        achievement.setUserId(userId);
        achievement.setAchievementType(type);
        achievement.setAchievementName(name);
        achievement.setDescription(description);
        achievement.setUnlockedAt(LocalDateTime.now());

        userAchievementMapper.insert(achievement);
        log.info("用户 {} 解锁成就: {}", userId, name);

        return achievement;
    }

    private List<UserAchievement> checkLikeMilestones(Long userId) {
        List<UserAchievement> result = new ArrayList<>();

        // 简化：检查不同点赞里程碑
        int[][] milestones = {{100, "获得100个赞"}, {500, "获得500个赞"}, {1000, "获得1000个赞"}, {10000, "获得10000个赞"}};
        String[] types = {"LIKE_100", "LIKE_500", "LIKE_1000", "LIKE_10000"};

        for (int i = 0; i < milestones.length; i++) {
            result.add(unlockAchievementIfNotExists(userId, types[i], milestones[i][1],
                    "你的内容累计获得了" + milestones[i][0] + "个赞！"));
        }

        return result;
    }

    private List<UserAchievement> checkFollowerMilestones(Long userId) {
        List<UserAchievement> result = new ArrayList<>();

        int[][] milestones = {{100, "拥有100个粉丝"}, {500, "拥有500个粉丝"}, {1000, "拥有1000个粉丝"}, {10000, "拥有10000个粉丝"}};
        String[] types = {"FOLLOWER_100", "FOLLOWER_500", "FOLLOWER_1000", "FOLLOWER_10000"};

        for (int i = 0; i < milestones.length; i++) {
            result.add(unlockAchievementIfNotExists(userId, types[i], milestones[i][1],
                    "你累计获得了" + milestones[i][0] + "个粉丝！"));
        }

        return result;
    }

    private List<UserAchievement> checkContinuousLogin(Long userId) {
        List<UserAchievement> result = new ArrayList<>();

        String[] types = {"LOGIN_7", "LOGIN_30", "LOGIN_90", "LOGIN_365"};
        String[] names = {"连续登录7天", "连续登录30天", "连续登录90天", "连续登录365天"};
        int[] days = {7, 30, 90, 365};

        for (int i = 0; i < types.length; i++) {
            result.add(unlockAchievementIfNotExists(userId, types[i], names[i],
                    "你已连续登录" + days[i] + "天！"));
        }

        return result;
    }

    private List<UserAchievement> checkContentMilestones(Long userId) {
        List<UserAchievement> result = new ArrayList<>();

        int[][] milestones = {{10, "发布10篇内容"}, {50, "发布50篇内容"}, {100, "发布100篇内容"}, {500, "发布500篇内容"}};
        String[] types = {"CONTENT_10", "CONTENT_50", "CONTENT_100", "CONTENT_500"};

        for (int i = 0; i < milestones.length; i++) {
            result.add(unlockAchievementIfNotExists(userId, types[i], milestones[i][1],
                    "你已累计发布了" + milestones[i][0] + "篇内容！"));
        }

        return result;
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementMapper.selectByUserId(userId);
    }

    @Override
    public List<OnboardingStep> getOnboardingSteps() {
        return onboardingStepMapper.selectList(
                new LambdaQueryWrapper<OnboardingStep>().orderByAsc(OnboardingStep::getStepOrder));
    }

    @Override
    public Map<String, Object> completeOnboardingStep(Long userId, Long stepId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 检查步骤是否存在
        OnboardingStep step = onboardingStepMapper.selectById(stepId);
        if (step == null) {
            result.put("success", false);
            result.put("message", "引导步骤不存在");
            return result;
        }

        // 检查是否已完成
        UserOnboardingProgress existing = userOnboardingProgressMapper.selectOne(
                new LambdaQueryWrapper<UserOnboardingProgress>()
                        .eq(UserOnboardingProgress::getUserId, userId)
                        .eq(UserOnboardingProgress::getStepId, stepId));

        if (existing != null && existing.getCompleted()) {
            result.put("success", true);
            result.put("message", "步骤已完成");
            result.put("stepId", stepId);
            return result;
        }

        if (existing != null) {
            // 更新为已完成
            existing.setCompleted(true);
            existing.setCompletedAt(LocalDateTime.now());
            userOnboardingProgressMapper.updateById(existing);
        } else {
            // 新建记录
            UserOnboardingProgress progress = new UserOnboardingProgress();
            progress.setUserId(userId);
            progress.setStepId(stepId);
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            userOnboardingProgressMapper.insert(progress);
        }

        result.put("success", true);
        result.put("message", "步骤完成成功");
        result.put("stepId", stepId);
        result.put("stepOrder", step.getStepOrder());
        result.put("actionType", step.getActionType());

        return result;
    }

    @Override
    public Map<String, Object> getUserOnboardingProgress(Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 获取所有引导步骤
        List<OnboardingStep> allSteps = getOnboardingSteps();
        int totalSteps = allSteps.size();

        // 获取用户已完成步骤
        List<UserOnboardingProgress> progressList = userOnboardingProgressMapper.selectByUserId(userId);
        Set<Long> completedStepIds = progressList.stream()
                .filter(UserOnboardingProgress::getCompleted)
                .map(UserOnboardingProgress::getStepId)
                .collect(Collectors.toSet());

        int completedCount = completedStepIds.size();
        boolean allCompleted = completedCount >= totalSteps;

        // 构建每步进度
        List<Map<String, Object>> stepProgress = new ArrayList<>();
        for (OnboardingStep step : allSteps) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stepId", step.getId());
            item.put("stepOrder", step.getStepOrder());
            item.put("title", step.getTitle());
            item.put("description", step.getDescription());
            item.put("imageUrl", step.getImageUrl());
            item.put("actionType", step.getActionType());
            item.put("targetUrl", step.getTargetUrl());
            item.put("completed", completedStepIds.contains(step.getId()));
            stepProgress.add(item);
        }

        result.put("userId", userId);
        result.put("totalSteps", totalSteps);
        result.put("completedSteps", completedCount);
        result.put("completionRate", totalSteps > 0 ?
                new java.math.BigDecimal(completedCount * 100.0 / totalSteps).setScale(2, java.math.RoundingMode.HALF_UP) :
                java.math.BigDecimal.ZERO);
        result.put("allCompleted", allCompleted);
        result.put("steps", stepProgress);

        return result;
    }
}
