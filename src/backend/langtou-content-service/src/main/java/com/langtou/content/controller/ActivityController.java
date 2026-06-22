package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Activity;
import com.langtou.content.entity.ActivityParticipant;
import com.langtou.content.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动接口（用户端）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@Tag(name = "活动服务", description = "活动管理接口")
    public class ActivityController {

    private final ActivityService activityService;

    /**
     * 在线活动列表
     * GET /api/v1/activities
     */
    @GetMapping
    public Result<PageResult<Activity>> listActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type) {
        PageResult<Activity> result = activityService.listOnlineActivities(page, size, type);
        return Result.success(result);
    }

    /**
     * 活动详情
     * GET /api/v1/activities/{id}
     */
    @GetMapping("/{id}")
    public Result<Activity> getActivityDetail(@PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);
        return Result.success(activity);
    }

    /**
     * 参与活动
     * POST /api/v1/activities/{id}/join
     */
    @PostMapping("/{id}/join")
    public Result<Void> joinActivity(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("请先登录");
        }
        activityService.joinActivity(id, userId);
        return Result.success("参与成功");
    }

    /**
     * 退出活动
     * POST /api/v1/activities/{id}/quit
     */
    @PostMapping("/{id}/quit")
    public Result<Void> quitActivity(@PathVariable Long id,
                                     @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("请先登录");
        }
        activityService.quitActivity(id, userId);
        return Result.success("退出成功");
    }

    /**
     * 活动排行榜
     * GET /api/v1/activities/{id}/ranking
     */
    @GetMapping("/{id}/ranking")
    public Result<List<ActivityParticipant>> getActivityRanking(
            @PathVariable Long id,
            @RequestParam(defaultValue = "note_count") String sortBy,
            @RequestParam(defaultValue = "50") Integer limit) {
        List<ActivityParticipant> ranking = activityService.getActivityRanking(id, sortBy, limit);
        return Result.success(ranking);
    }
}
