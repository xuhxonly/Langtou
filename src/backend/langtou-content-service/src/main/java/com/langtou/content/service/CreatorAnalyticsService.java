package com.langtou.content.service;

import com.langtou.content.dto.CreatorDashboardVO;

import java.util.List;
import java.util.Map;

public interface CreatorAnalyticsService {
    CreatorDashboardVO getDashboard(Long userId);
    CreatorDashboardVO.TrendData getTrend(Long userId, int days);
    List<CreatorDashboardVO.NoteRanking> getNoteRanking(Long userId, int limit);
    CreatorDashboardVO.FanProfile getFanProfile(Long userId);

    /**
     * 获取单篇内容详细分析（流量来源分布、阅读时长、互动转化漏斗）
     */
    Map<String, Object> getContentAnalytics(Long contentId);

    /**
     * 获取内容转化漏斗（曝光->点击->阅读->互动->分享）
     */
    Map<String, Object> getContentTrafficFunnel(Long contentId, String dateRange);

    /**
     * 获取创作者流量来源汇总
     */
    Map<String, Object> getCreatorTrafficSources(Long creatorId, String dateRange);

    /**
     * 获取创作者内容诊断（各内容表现对比、优化建议）
     */
    Map<String, Object> getCreatorContentDiagnosis(Long creatorId);

    /**
     * 获取创作者每日统计趋势
     */
    List<Map<String, Object>> getCreatorDailyStats(Long creatorId, String startDate, String endDate);
}
