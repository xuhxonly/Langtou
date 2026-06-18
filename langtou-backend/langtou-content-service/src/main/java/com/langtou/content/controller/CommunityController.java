package com.langtou.content.controller;

import com.langtou.common.result.Result;
import com.langtou.content.entity.UserAchievement;
import com.langtou.content.entity.UserLevel;
import com.langtou.content.entity.OnboardingStep;
import com.langtou.content.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 获取所有等级配置
     */
    @GetMapping("/levels")
    public Result<List<UserLevel>> getAllLevels() {
        return Result.success(communityService.getAllLevels());
    }

    /**
     * 获取我的等级
     */
    @GetMapping("/my-level")
    public Result<Map<String, Object>> getMyLevel(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(communityService.getUserLevel(userId));
    }

    /**
     * 获取我的成就列表
     */
    @GetMapping("/achievements")
    public Result<List<UserAchievement>> getMyAchievements(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(communityService.getUserAchievements(userId));
    }

    /**
     * 获取新手引导步骤
     */
    @GetMapping("/onboarding")
    public Result<List<OnboardingStep>> getOnboardingSteps() {
        return Result.success(communityService.getOnboardingSteps());
    }

    /**
     * 完成引导步骤
     */
    @PostMapping("/onboarding/{stepId}/complete")
    public Result<Map<String, Object>> completeOnboardingStep(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long stepId) {
        return Result.success(communityService.completeOnboardingStep(userId, stepId));
    }

    /**
     * 获取引导进度
     */
    @GetMapping("/onboarding/progress")
    public Result<Map<String, Object>> getOnboardingProgress(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(communityService.getUserOnboardingProgress(userId));
    }
}
