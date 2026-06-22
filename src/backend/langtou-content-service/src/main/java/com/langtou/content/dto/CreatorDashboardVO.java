package com.langtou.content.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CreatorDashboardVO {
    private long totalViews;        // 总浏览量
    private long totalLikes;        // 总点赞数
    private long totalCollects;    // 总收藏数
    private long totalShares;      // 总分享数
    private long totalComments;    // 总评论数
    private int notesCount;        // 笔记数量
    private int followersCount;    // 粉丝数量

    @Data
    public static class TrendData {
        private List<String> dates;           // 日期列表 ["06-05", "06-06", ...]
        private List<Long> views;            // 每日浏览量
        private List<Long> likes;            // 每日点赞
        private List<Long> collects;         // 每日收藏
        private List<Long> shares;           // 每日分享
        private List<Long> comments;         // 每日评论
    }

    @Data
    public static class NoteRanking {
        private Long noteId;
        private String title;
        private long views;
        private long likes;
        private long collects;
        private long comments;
    }

    @Data
    public static class FanProfile {
        private Map<String, Integer> genderDistribution;   // {"male": 42, "female": 53, "unknown": 5}
        private List<Map<String, Object>> topRegions;      // [{"name": "北京", "count": 120}, ...]
    }
}
