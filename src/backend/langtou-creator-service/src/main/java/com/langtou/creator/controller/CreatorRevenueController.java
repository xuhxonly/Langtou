package com.langtou.creator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.creator.entity.CreatorAdRevenue;
import com.langtou.creator.service.CreatorMonetizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/creator/revenue")
@RequiredArgsConstructor
@Tag(name = "创作者收益", description = "创作者收益接口")
    public class CreatorRevenueController {

    private final CreatorMonetizationService monetizationService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getRevenueOverview(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(monetizationService.getRevenueOverview(userId));
    }

    @GetMapping("/details")
    public Result<PageResult<CreatorAdRevenue>> getRevenueDetails(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(monetizationService.getRevenueDetails(userId, page, size));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getRevenueTrend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "day") String period) {
        return Result.success(monetizationService.getRevenueTrend(userId, period));
    }
}
