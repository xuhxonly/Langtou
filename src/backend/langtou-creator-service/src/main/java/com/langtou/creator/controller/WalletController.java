package com.langtou.creator.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.Result;
import com.langtou.creator.entity.CreatorWallet;
import com.langtou.creator.entity.WithdrawalRequest;
import com.langtou.creator.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/creator/wallet")
@RequiredArgsConstructor
@Tag(name = "创作者钱包", description = "创作者钱包接口")
    public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Result<CreatorWallet> getWallet(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(walletService.getOrCreateWallet(userId));
    }

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
