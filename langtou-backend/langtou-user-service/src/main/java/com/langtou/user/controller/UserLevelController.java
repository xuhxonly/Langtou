package com.langtou.user.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.user.dto.UserAchievementDTO;
import com.langtou.user.dto.UserLevelDTO;
import com.langtou.user.entity.PointsRecord;
import com.langtou.user.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户成长体系控制器
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserLevelController {

    private final UserLevelService userLevelService;

    /**
     * 获取当前用户等级/积分信息
     */
    @GetMapping("/me/level")
    public Result<UserLevelDTO> getMyLevel(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        UserLevelDTO levelDTO = userLevelService.getUserLevel(userId);
        return Result.success(levelDTO);
    }

    /**
     * 获取指定用户等级/积分信息
     */
    @GetMapping("/{userId}/level")
    public Result<UserLevelDTO> getUserLevel(@PathVariable Long userId) {
        UserLevelDTO levelDTO = userLevelService.getUserLevel(userId);
        return Result.success(levelDTO);
    }

    /**
     * 获取当前用户成就列表
     */
    @GetMapping("/me/achievements")
    public Result<List<UserAchievementDTO>> getMyAchievements(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<UserAchievementDTO> achievements = userLevelService.getUserAchievements(userId);
        return Result.success(achievements);
    }

    /**
     * 获取指定用户成就列表
     */
    @GetMapping("/{userId}/achievements")
    public Result<List<UserAchievementDTO>> getUserAchievements(@PathVariable Long userId) {
        List<UserAchievementDTO> achievements = userLevelService.getUserAchievements(userId);
        return Result.success(achievements);
    }

    /**
     * 获取当前用户积分记录
     */
    @GetMapping("/me/points-records")
    public Result<List<PointsRecord>> getMyPointsRecords(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<PointsRecord> records = userLevelService.getPointsRecords(userId, limit);
        return Result.success(records);
    }

    /**
     * 获取指定用户积分记录
     */
    @GetMapping("/{userId}/points-records")
    public Result<List<PointsRecord>> getUserPointsRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<PointsRecord> records = userLevelService.getPointsRecords(userId, limit);
        return Result.success(records);
    }
}
