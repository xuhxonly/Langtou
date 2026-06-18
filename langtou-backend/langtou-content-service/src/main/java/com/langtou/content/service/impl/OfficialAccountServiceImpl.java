package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.OfficialAccount;
import com.langtou.content.mapper.OfficialAccountMapper;
import com.langtou.content.service.OfficialAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficialAccountServiceImpl implements OfficialAccountService {

    private final OfficialAccountMapper officialAccountMapper;

    @Override
    public OfficialAccount createAccount(OfficialAccount account) {
        if (account.getUserId() == null) {
            throw new BusinessException("关联用户ID不能为空");
        }
        if (!StringUtils.hasText(account.getDisplayName())) {
            throw new BusinessException("显示名称不能为空");
        }
        if (!StringUtils.hasText(account.getAvatarUrl())) {
            throw new BusinessException("头像URL不能为空");
        }
        if (account.getAccountType() == null) {
            account.setAccountType("OFFICIAL");
        }
        if (account.getVerifiedBadge() == null) {
            account.setVerifiedBadge("BLUE_V");
        }
        if (account.getStatus() == null) {
            account.setStatus("ACTIVE");
        }

        // 检查用户ID是否已绑定官方账号
        Long count = officialAccountMapper.selectCount(
                new QueryWrapper<OfficialAccount>().eq("user_id", account.getUserId())
        );
        if (count != null && count > 0) {
            throw new BusinessException("该用户已绑定官方账号");
        }

        officialAccountMapper.insert(account);
        log.info("创建官方账号成功: id={}, displayName={}", account.getId(), account.getDisplayName());
        return account;
    }

    @Override
    public OfficialAccount updateAccount(Long accountId, OfficialAccount account) {
        OfficialAccount existing = officialAccountMapper.selectById(accountId);
        if (existing == null) {
            throw new BusinessException("官方账号不存在: " + accountId);
        }

        account.setId(accountId);
        // 保护不可修改字段
        account.setUserId(null);
        account.setStatus(null);

        officialAccountMapper.updateById(account);
        log.info("更新官方账号成功: id={}", accountId);
        return officialAccountMapper.selectById(accountId);
    }

    @Override
    public OfficialAccount reviewAccount(Long accountId, String action) {
        OfficialAccount existing = officialAccountMapper.selectById(accountId);
        if (existing == null) {
            throw new BusinessException("官方账号不存在: " + accountId);
        }

        if ("approve".equals(action)) {
            existing.setStatus("ACTIVE");
        } else if ("suspend".equals(action)) {
            existing.setStatus("SUSPENDED");
        } else {
            throw new BusinessException("无效的审核操作: " + action);
        }

        officialAccountMapper.updateById(existing);
        log.info("审核官方账号: id={}, action={}", accountId, action);
        return existing;
    }

    @Override
    public PageResult<OfficialAccount> listAccounts(Integer page, Integer size, String accountType, String status) {
        Page<OfficialAccount> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        QueryWrapper<OfficialAccount> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(accountType)) {
            wrapper.eq("account_type", accountType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }

        wrapper.orderByDesc("created_at");

        Page<OfficialAccount> resultPage = officialAccountMapper.selectPage(pageParam, wrapper);
        return PageResult.of(resultPage);
    }

    @Override
    public OfficialAccount getAccountDetail(Long accountId) {
        OfficialAccount account = officialAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException("官方账号不存在: " + accountId);
        }
        return account;
    }
}
