package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.CreatorCommission;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CommissionService {

    /**
     * 记录佣金
     */
    CreatorCommission recordCommission(Long creatorId, Long productId, Long noteId,
                                        String orderNo, Long buyerId,
                                        BigDecimal amount, BigDecimal commissionAmount);

    /**
     * 获取创作者收益明细（分页）
     */
    PageResult<CreatorCommission> getCommissionList(Long creatorId, int page, int size);

    /**
     * 创作者收益汇总
     */
    Map<String, Object> getCommissionSummary(Long creatorId);

    /**
     * 创作者收益趋势（日/周/月）
     */
    List<Map<String, Object>> getCommissionTrend(Long creatorId, String period);

    /**
     * 提现申请
     */
    void requestWithdraw(Long creatorId, BigDecimal amount);
}
