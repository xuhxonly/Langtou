package com.langtou.creator.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreatorDashboardVO {
    private long totalViews;
    private long totalLikes;
    private long totalCollects;
    private long totalShares;
    private long totalComments;
    private int notesCount;
    private int followersCount;

    @Data
    public static class TrendData {
        private List<String> dates;
        private List<Long> views;
        private List<Long> likes;
        private List<Long> collects;
        private List<Long> shares;
        private List<Long> comments;
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
        private Map<String, Integer> genderDistribution;
        private List<Map<String, Object>> topRegions;
    }
}
