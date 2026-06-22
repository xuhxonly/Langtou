package com.langtou.creator.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.creator.entity.CreatorWallet;
import com.langtou.creator.entity.WithdrawalRequest;
import com.langtou.creator.mapper.CreatorWalletMapper;
import com.langtou.creator.mapper.WithdrawalRequestMapper;
import com.langtou.creator.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final CreatorWalletMapper walletMapper;
    private final WithdrawalRequestMapper withdrawalRequestMapper;

    @Override
    public CreatorWallet getWallet(Long creatorId) {
        CreatorWallet wallet = walletMapper.selectByCreatorId(creatorId);
        if (wallet == null) {
            throw new BusinessException("钱包不存在");
        }
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorWallet getOrCreateWallet(Long creatorId) {
        CreatorWallet wallet = walletMapper.selectByCreatorId(creatorId);
        if (wallet == null) {
            wallet = new CreatorWallet();
            wallet.setCreatorId(creatorId);
            wallet.setTotalRevenue(BigDecimal.ZERO);
            wallet.setAvailableBalance(BigDecimal.ZERO);
            wallet.setPendingAmount(BigDecimal.ZERO);
            wallet.setWithdrawnAmount(BigDecimal.ZERO);
            walletMapper.insert(wallet);
            log.info("创作者钱包创建: creatorId={}", creatorId);
        }
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRequest requestWithdraw(Long creatorId, BigDecimal amount, String bankAccount, String realName) {
        if (amount == null || amount.doubleValue() <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }
        if (!StringUtils.hasText(bankAccount)) {
            throw new BusinessException("银行账号不能为空");
        }
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException("真实姓名不能为空");
        }

        CreatorWallet wallet = getOrCreateWallet(creatorId);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException("可用余额不足");
        }

        WithdrawalRequest request = new WithdrawalRequest();
        request.setCreatorId(creatorId);
        request.setAmount(amount);
        request.setStatus("PENDING");
        request.setBankAccount(bankAccount);
        request.setRealName(realName);

        withdrawalRequestMapper.insert(request);

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setPendingAmount(wallet.getPendingAmount().add(amount));
        walletMapper.updateById(wallet);

        log.info("提现申请: creatorId={}, amount={}", creatorId, amount);
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveWithdrawal(Long withdrawalId) {
        WithdrawalRequest request = withdrawalRequestMapper.selectById(withdrawalId);
        if (request == null) {
            throw new BusinessException("提现申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now());
        withdrawalRequestMapper.updateById(request);

        CreatorWallet wallet = getOrCreateWallet(request.getCreatorId());
        wallet.setPendingAmount(wallet.getPendingAmount().subtract(request.getAmount()));
        wallet.setWithdrawnAmount(wallet.getWithdrawnAmount().add(request.getAmount()));
        walletMapper.updateById(wallet);

        log.info("提现批准: withdrawalId={}, creatorId={}, amount={}", withdrawalId, request.getCreatorId(), request.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdrawal(Long withdrawalId, String remark) {
        WithdrawalRequest request = withdrawalRequestMapper.selectById(withdrawalId);
        if (request == null) {
            throw new BusinessException("提现申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(LocalDateTime.now());
        request.setRemark(remark);
        withdrawalRequestMapper.updateById(request);

        CreatorWallet wallet = getOrCreateWallet(request.getCreatorId());
        wallet.setPendingAmount(wallet.getPendingAmount().subtract(request.getAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(request.getAmount()));
        walletMapper.updateById(wallet);

        log.info("提现拒绝: withdrawalId={}, creatorId={}, amount={}, remark={}", withdrawalId, request.getCreatorId(), request.getAmount(), remark);
    }

    @Override
    public List<WithdrawalRequest> getWithdrawalList(String status) {
        if (StringUtils.hasText(status)) {
            return withdrawalRequestMapper.selectByStatus(status);
        }
        return withdrawalRequestMapper.selectList(null);
    }
}
