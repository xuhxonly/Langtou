package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.Result;
import com.langtou.content.dto.CreatorDashboardVO;
import com.langtou.content.service.CreatorAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/creator")
@RequiredArgsConstructor
@Tag(name = "创作者数据分析", description = "创作者数据分析接口")
    public class CreatorAnalyticsController {

    private final CreatorAnalyticsService creatorAnalyticsService;

    @GetMapping("/dashboard")
    public Result<CreatorDashboardVO> getDashboard(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(creatorAnalyticsService.getDashboard(userId));
    }

    @GetMapping("/dashboard/trend")
    public Result<CreatorDashboardVO.TrendData> getTrend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(creatorAnalyticsService.getTrend(userId, days));
    }

    @GetMapping("/dashboard/note-ranking")
    public Result<List<CreatorDashboardVO.NoteRanking>> getNoteRanking(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        return Result.success(creatorAnalyticsService.getNoteRanking(userId, limit));
    }

    @GetMapping("/dashboard/fan-profile")
    public Result<CreatorDashboardVO.FanProfile> getFanProfile(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(creatorAnalyticsService.getFanProfile(userId));
    }

    // ==================== 增强分析接口 ====================

    @GetMapping("/analytics/content/{contentId}")
    public Result<Map<String, Object>> getContentAnalytics(@PathVariable Long contentId) {
        return Result.success(creatorAnalyticsService.getContentAnalytics(contentId));
    }

    @GetMapping("/analytics/funnel/{contentId}")
    public Result<Map<String, Object>> getContentTrafficFunnel(
            @PathVariable Long contentId,
            @RequestParam(defaultValue = "LAST_7_DAYS") String dateRange) {
        return Result.success(creatorAnalyticsService.getContentTrafficFunnel(contentId, dateRange));
    }

    @GetMapping("/analytics/traffic-sources")
    public Result<Map<String, Object>> getCreatorTrafficSources(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "LAST_7_DAYS") String dateRange) {
        return Result.success(creatorAnalyticsService.getCreatorTrafficSources(userId, dateRange));
    }

    @GetMapping("/analytics/diagnosis")
    public Result<Map<String, Object>> getCreatorContentDiagnosis(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(creatorAnalyticsService.getCreatorContentDiagnosis(userId));
    }

    @GetMapping("/analytics/daily-stats")
    public Result<List<Map<String, Object>>> getCreatorDailyStats(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(creatorAnalyticsService.getCreatorDailyStats(userId, startDate, endDate));
    }
}
