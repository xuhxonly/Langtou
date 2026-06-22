package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Activity;
import com.langtou.content.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 活动管理接口（管理员端）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/activities")
@RequiredArgsConstructor
@RequireRole("ADMIN")
@Tag(name = "活动管理（管理员）", description = "管理员活动接口")
    public class AdminActivityController {

    private final ActivityService activityService;

    /**
     * 创建活动
     * POST /api/v1/admin/activities
     */
    @PostMapping
    public Result<Activity> createActivity(@RequestBody Activity activity) {
        Activity created = activityService.createActivity(activity, null);
        return Result.success(created);
    }

    /**
     * 更新活动
     * PUT /api/v1/admin/activities/{id}
     */
    @PutMapping("/{id}")
    public Result<Activity> updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        Activity updated = activityService.updateActivity(id, activity, null);
        return Result.success(updated);
    }

    /**
     * 删除活动
     * DELETE /api/v1/admin/activities/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return Result.success("删除成功");
    }

    /**
     * 发布活动（DRAFT -> PENDING_REVIEW -> ONLINE）
     * PUT /api/v1/admin/activities/{id}/publish
     */
    @PutMapping("/{id}/publish")
    public Result<Activity> publishActivity(@PathVariable Long id) {
        Activity published = activityService.publishActivity(id);
        return Result.success(published);
    }

    /**
     * 结束活动
     * PUT /api/v1/admin/activities/{id}/end
     */
    @PutMapping("/{id}/end")
    public Result<Activity> endActivity(@PathVariable Long id) {
        Activity ended = activityService.endActivity(id);
        return Result.success(ended);
    }

    /**
     * 活动列表（全状态）
     * GET /api/v1/admin/activities
     */
    @GetMapping
    public Result<PageResult<Activity>> listActivities(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        PageResult<Activity> result = activityService.listAllActivities(page, size, status, type, keyword);
        return Result.success(result);
    }

    /**
     * 活动统计数据
     * GET /api/v1/admin/activities/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getActivityStats(@PathVariable Long id) {
        Map<String, Object> stats = activityService.getActivityStats(id);
        return Result.success(stats);
    }
}
