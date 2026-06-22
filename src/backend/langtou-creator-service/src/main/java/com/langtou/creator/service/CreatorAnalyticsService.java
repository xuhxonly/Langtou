package com.langtou.creator.service;

import com.langtou.creator.dto.CreatorDashboardVO;

import java.util.List;
import java.util.Map;

public interface CreatorAnalyticsService {

    CreatorDashboardVO getDashboard(Long userId);

    CreatorDashboardVO.TrendData getTrend(Long userId, int days);

    List<CreatorDashboardVO.NoteRanking> getNoteRanking(Long userId, int limit);

    CreatorDashboardVO.FanProfile getFanProfile(Long userId);

    Map<String, Object> getContentAnalytics(Long contentId);

    Map<String, Object> getContentTrafficFunnel(Long contentId, String dateRange);

    Map<String, Object> getCreatorTrafficSources(Long creatorId, String dateRange);

    Map<String, Object> getCreatorContentDiagnosis(Long creatorId);

    List<Map<String, Object>> getCreatorDailyStats(Long creatorId, String startDate, String endDate);
}
