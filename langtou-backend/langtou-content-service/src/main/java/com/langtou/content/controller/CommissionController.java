package com.langtou.content.controller;

import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.CreatorCommission;
import com.langtou.content.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 创作者收益 - 佣金管理
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/creator/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    /**
     * 收益明细
     * GET /api/v1/creator/commissions
     */
    @GetMapping
    public Result<PageResult<CreatorCommission>> getCommissionList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<CreatorCommission> result = commissionService.getCommissionList(userId, page, size);
        return Result.success(result);
    }

    /**
     * 收益汇总
     * GET /api/v1/creator/commissions/summary
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getCommissionSummary(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> result = commissionService.getCommissionSummary(userId);
        return Result.success(result);
    }

    /**
     * 收益趋势（日/周/月）
     * GET /api/v1/creator/commissions/trend
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getCommissionTrend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "day") String period) {
        List<Map<String, Object>> result = commissionService.getCommissionTrend(userId, period);
        return Result.success(result);
    }

    /**
     * 提现申请
     * POST /api/v1/creator/commissions/withdraw
     */
    @PostMapping("/withdraw")
    public Result<Void> requestWithdraw(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        commissionService.requestWithdraw(userId, amount);
        return Result.success("提现申请已提交");
    }
}
