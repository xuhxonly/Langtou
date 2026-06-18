package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.CreatorAdRevenue;

import java.util.List;
import java.util.Map;

public interface CreatorMonetizationService {

    /**
     * 记录广告曝光
     */
    void recordImpression(Long creatorId, Long noteId);

    /**
     * 记录广告点击
     */
    void recordClick(Long creatorId, Long noteId);

    /**
     * 计算广告收益（曝光单价0.01元，点击单价0.1元）
     */
    void calculateRevenue(Long creatorId, Long noteId);

    /**
     * 日结算
     */
    void dailySettlement();

    /**
     * 收益概览
     */
    Map<String, Object> getRevenueOverview(Long creatorId);

    /**
     * 收益明细（分页）
     */
    PageResult<CreatorAdRevenue> getRevenueDetails(Long creatorId, int page, int size);

    /**
     * 收益趋势（日/周/月）
     */
    List<Map<String, Object>> getRevenueTrend(Long creatorId, String period);
}
