package com.langtou.creator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.creator.entity.CreatorCommission;
import com.langtou.creator.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/creator/commissions")
@RequiredArgsConstructor
@Tag(name = "创作者佣金", description = "创作者佣金接口")
    public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public Result<PageResult<CreatorCommission>> getCommissionList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(commissionService.getCommissionList(userId, page, size));
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> getCommissionSummary(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(commissionService.getCommissionSummary(userId));
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getCommissionTrend(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "day") String period) {
        return Result.success(commissionService.getCommissionTrend(userId, period));
    }

    @PostMapping("/withdraw")
    public Result<Void> requestWithdraw(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        commissionService.requestWithdraw(userId, amount);
        return Result.success("提现申请已提交");
    }
}
