package com.langtou.content.controller;

import com.langtou.common.result.Result;
import com.langtou.content.entity.CreatorWallet;
import com.langtou.content.entity.WithdrawalRequest;
import com.langtou.content.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 创作者钱包
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/creator/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * 钱包信息
     * GET /api/v1/creator/wallet
     */
    @GetMapping
    public Result<CreatorWallet> getWallet(@RequestHeader("X-User-Id") Long userId) {
        CreatorWallet wallet = walletService.getOrCreateWallet(userId);
        return Result.success(wallet);
    }

    /**
     * 提现申请
     * POST /api/v1/creator/wallet/withdraw
     */
    @PostMapping("/withdraw")
    public Result<WithdrawalRequest> requestWithdraw(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestBody Map<String, String> body) {
        BigDecimal amount = new BigDecimal(body.get("amount"));
        String bankAccount = body.get("bankAccount");
        String realName = body.get("realName");
        WithdrawalRequest result = walletService.requestWithdraw(userId, amount, bankAccount, realName);
        return Result.success("提现申请已提交", result);
    }
}
