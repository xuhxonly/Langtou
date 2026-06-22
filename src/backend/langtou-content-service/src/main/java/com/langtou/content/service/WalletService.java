package com.langtou.content.service;

import com.langtou.content.entity.CreatorWallet;
import com.langtou.content.entity.WithdrawalRequest;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    /**
     * 查询钱包信息
     */
    CreatorWallet getWallet(Long creatorId);

    /**
     * 获取或创建钱包
     */
    CreatorWallet getOrCreateWallet(Long creatorId);

    /**
     * 提现申请
     */
    WithdrawalRequest requestWithdraw(Long creatorId, BigDecimal amount, String bankAccount, String realName);

    /**
     * 提现审核（管理员）
     */
    void approveWithdrawal(Long withdrawalId);

    /**
     * 拒绝提现（管理员）
     */
    void rejectWithdrawal(Long withdrawalId, String remark);

    /**
     * 获取提现列表（管理员）
     */
    List<WithdrawalRequest> getWithdrawalList(String status);
}
