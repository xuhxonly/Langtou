package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.Result;
import com.langtou.content.entity.WithdrawalRequest;
import com.langtou.content.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员 - 提现审核
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/withdrawals")
@RequiredArgsConstructor
@Tag(name = "钱包管理（管理员）", description = "管理员钱包接口")
    public class AdminWalletController {

    private final WalletService walletService;

    /**
     * 提现列表
     * GET /api/v1/admin/withdrawals
     */
    @GetMapping
    public Result<List<WithdrawalRequest>> getWithdrawalList(
            @RequestParam(required = false) String status) {
        List<WithdrawalRequest> result = walletService.getWithdrawalList(status);
        return Result.success(result);
    }

    /**
     * 批准提现
     * PUT /api/v1/admin/withdrawals/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public Result<Void> approveWithdrawal(@PathVariable Long id) {
        walletService.approveWithdrawal(id);
        return Result.success("提现已批准");
    }

    /**
     * 拒绝提现
     * PUT /api/v1/admin/withdrawals/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public Result<Void> rejectWithdrawal(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, String> body) {
        String remark = body != null ? body.get("remark") : null;
        walletService.rejectWithdrawal(id, remark);
        return Result.success("提现已拒绝");
    }
}
