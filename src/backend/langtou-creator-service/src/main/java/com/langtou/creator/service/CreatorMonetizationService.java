package com.langtou.creator.service;

import com.langtou.common.result.PageResult;
import com.langtou.creator.entity.CreatorAdRevenue;

import java.util.List;
import java.util.Map;

public interface CreatorMonetizationService {

    void recordImpression(Long creatorId, Long noteId);

    void recordClick(Long creatorId, Long noteId);

    void calculateRevenue(Long creatorId, Long noteId);

    void dailySettlement();

    Map<String, Object> getRevenueOverview(Long creatorId);

    PageResult<CreatorAdRevenue> getRevenueDetails(Long creatorId, int page, int size);

    List<Map<String, Object>> getRevenueTrend(Long creatorId, String period);
}
