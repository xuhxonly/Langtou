package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.CreatorAdRevenue;
import com.langtou.content.service.CreatorMonetizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 创作者广告收益
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/creator/revenue")
@RequiredArgsConstructor
@Tag(name = "创作者收益", description = "创作者收益接口")
    public class CreatorRevenueController {

    private final CreatorMonetizationService monetizationService;

    /**
     * 收益概览
     * GET /api/v1/creator/revenue/overview
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getRevenueOverview(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> result = monetizationService.getRevenueOverview(userId);
        return Result.success(result);
    }

    /**
     * 收益明细（分页）
     * GET /api/v1/creator/revenue/details
     */
    @GetMapping("/details")
    public Result<PageResult<CreatorAdRevenue>> getRevenueDetails(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<CreatorAdRevenue> result = monetizationService.getRevenueDetails(userId, page, size);
        return Result.success(result);
    }

    /**
     * 收益趋势（日/周/月）
     * GET /api/v1/creator/revenue/trend
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getRevenueTrend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "day") String period) {
        List<Map<String, Object>> result = monetizationService.getRevenueTrend(userId, period);
        return Result.success(result);
    }
}
