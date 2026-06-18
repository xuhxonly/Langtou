package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.OfficialAccount;

/**
 * 官方账号服务接口
 */
public interface OfficialAccountService {

    /**
     * 创建官方账号
     */
    OfficialAccount createAccount(OfficialAccount account);

    /**
     * 更新官方账号
     */
    OfficialAccount updateAccount(Long accountId, OfficialAccount account);

    /**
     * 认证审核（通过/拒绝）
     */
    OfficialAccount reviewAccount(Long accountId, String action);

    /**
     * 官方账号列表
     */
    PageResult<OfficialAccount> listAccounts(Integer page, Integer size, String accountType, String status);

    /**
     * 获取官方账号详情
     */
    OfficialAccount getAccountDetail(Long accountId);
}
