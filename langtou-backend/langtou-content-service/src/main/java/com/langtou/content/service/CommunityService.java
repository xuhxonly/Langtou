package com.langtou.content.service;

import com.langtou.content.entity.UserAchievement;
import com.langtou.content.entity.UserLevel;
import com.langtou.content.entity.OnboardingStep;

import java.util.List;
import java.util.Map;

public interface CommunityService {

    /**
     * 获取用户等级
     */
    Map<String, Object> getUserLevel(Long userId);

    /**
     * 获取所有等级配置
     */
    List<UserLevel> getAllLevels();

    /**
     * 检查并解锁成就
     */
    List<UserAchievement> checkAndUnlockAchievements(Long userId, String eventType);

    /**
     * 获取用户成就列表
     */
    List<UserAchievement> getUserAchievements(Long userId);

    /**
     * 获取新手引导步骤
     */
    List<OnboardingStep> getOnboardingSteps();

    /**
     * 完成引导步骤
     */
    Map<String, Object> completeOnboardingStep(Long userId, Long stepId);

    /**
     * 获取引导进度
     */
    Map<String, Object> getUserOnboardingProgress(Long userId);
}
