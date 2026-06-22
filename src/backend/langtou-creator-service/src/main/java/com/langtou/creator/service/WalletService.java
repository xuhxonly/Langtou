package com.langtou.creator.service;

import com.langtou.creator.entity.CreatorWallet;
import com.langtou.creator.entity.WithdrawalRequest;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    CreatorWallet getWallet(Long creatorId);

    CreatorWallet getOrCreateWallet(Long creatorId);

    WithdrawalRequest requestWithdraw(Long creatorId, BigDecimal amount, String bankAccount, String realName);

    void approveWithdrawal(Long withdrawalId);

    void rejectWithdrawal(Long withdrawalId, String remark);

    List<WithdrawalRequest> getWithdrawalList(String status);
}
