package com.langtou.creator.service;

import com.langtou.common.result.PageResult;
import com.langtou.creator.entity.CreatorCommission;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CommissionService {

    CreatorCommission recordCommission(Long creatorId, Long productId, Long noteId,
                                       String orderNo, Long buyerId,
                                       BigDecimal amount, BigDecimal commissionAmount);

    PageResult<CreatorCommission> getCommissionList(Long creatorId, int page, int size);

    Map<String, Object> getCommissionSummary(Long creatorId);

    List<Map<String, Object>> getCommissionTrend(Long creatorId, String period);

    void requestWithdraw(Long creatorId, BigDecimal amount);
}
