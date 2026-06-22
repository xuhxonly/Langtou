package com.langtou.creator.service.impl;

import com.langtou.creator.dto.CreatorDashboardVO;
import com.langtou.creator.entity.CreatorDailyStats;
import com.langtou.creator.mapper.CreatorDailyStatsMapper;
import com.langtou.creator.mapper.UserProfileMapper;
import com.langtou.creator.service.CreatorAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorAnalyticsServiceImpl implements CreatorAnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final CreatorDailyStatsMapper creatorDailyStatsMapper;
    private final UserProfileMapper userProfileMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter ISO_DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public CreatorDashboardVO getDashboard(Long userId) {
        CreatorDashboardVO dashboard = new CreatorDashboardVO();

        try {
            Map<String, Object> agg = jdbcTemplate.queryForMap(
                    "SELECT COALESCE(SUM(view_count), 0) AS total_views, " +
                    "COALESCE(SUM(like_count), 0) AS total_likes, " +
                    "COALESCE(SUM(collect_count), 0) AS total_collects, " +
                    "COALESCE(SUM(share_count), 0) AS total_shares, " +
                    "COALESCE(SUM(comment_count), 0) AS total_comments, " +
                    "COUNT(*) AS notes_count " +
                    "FROM note WHERE user_id = ? AND status = 1 AND deleted = 0", userId);
            dashboard.setTotalViews(((Number) agg.get("total_views")).longValue());
            dashboard.setTotalLikes(((Number) agg.get("total_likes")).longValue());
            dashboard.setTotalCollects(((Number) agg.get("total_collects")).longValue());
            dashboard.setTotalShares(((Number) agg.get("total_shares")).longValue());
            dashboard.setTotalComments(((Number) agg.get("total_comments")).longValue());
            dashboard.setNotesCount(((Number) agg.get("notes_count")).intValue());
        } catch (Exception e) {
            log.warn("聚合创作者仪表盘数据失败, userId={}, error={}", userId, e.getMessage());
            dashboard.setTotalViews(0);
            dashboard.setTotalLikes(0);
            dashboard.setTotalCollects(0);
            dashboard.setTotalShares(0);
            dashboard.setTotalComments(0);
            dashboard.setNotesCount(0);
        }

        try {
            Integer followerCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM follow WHERE user_id = ?", Integer.class, userId);
            dashboard.setFollowersCount(followerCount != null ? followerCount : 0);
        } catch (Exception e) {
            log.warn("获取粉丝数失败, userId={}, error={}", userId, e.getMessage());
            dashboard.setFollowersCount(0);
        }

        return dashboard;
    }

    @Override
    public CreatorDashboardVO.TrendData getTrend(Long userId, int days) {
        CreatorDashboardVO.TrendData trendData = new CreatorDashboardVO.TrendData();

        List<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            dates.add(today.minusDays(i).format(DATE_FMT));
        }
        trendData.setDates(dates);

        List<Long> views = new ArrayList<>(Collections.nCopies(days, 0L));
        List<Long> likes = new ArrayList<>(Collections.nCopies(days, 0L));
        List<Long> collects = new ArrayList<>(Collections.nCopies(days, 0L));
        List<Long> shares = new ArrayList<>(Collections.nCopies(days, 0L));
        List<Long> comments = new ArrayList<>(Collections.nCopies(days, 0L));

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) AS date, event_name, COUNT(*) AS cnt " +
                    "FROM analytics_event " +
                    "WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "GROUP BY DATE(created_at), event_name " +
                    "ORDER BY date", userId, days);

            for (Map<String, Object> row : rows) {
                String dateStr = ((java.sql.Date) row.get("date")).toLocalDate().format(DATE_FMT);
                String eventName = (String) row.get("event_name");
                long cnt = ((Number) row.get("cnt")).longValue();
                int idx = dates.indexOf(dateStr);
                if (idx < 0) continue;

                switch (eventName != null ? eventName.toLowerCase() : "") {
                    case "view":
                    case "page_view":
                        views.set(idx, views.get(idx) + cnt);
                        break;
                    case "like":
                        likes.set(idx, likes.get(idx) + cnt);
                        break;
                    case "collect":
                    case "favorite":
                        collects.set(idx, collects.get(idx) + cnt);
                        break;
                    case "share":
                        shares.set(idx, shares.get(idx) + cnt);
                        break;
                    case "comment":
                        comments.set(idx, comments.get(idx) + cnt);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.warn("获取趋势数据失败, userId={}, days={}, error={}", userId, days, e.getMessage());
        }

        trendData.setViews(views);
        trendData.setLikes(likes);
        trendData.setCollects(collects);
        trendData.setShares(shares);
        trendData.setComments(comments);

        return trendData;
    }

    @Override
    public List<CreatorDashboardVO.NoteRanking> getNoteRanking(Long userId, int limit) {
        List<CreatorDashboardVO.NoteRanking> rankings = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, title, view_count, like_count, collect_count, comment_count " +
                    "FROM note " +
                    "WHERE user_id = ? AND status = 1 AND deleted = 0 " +
                    "ORDER BY (like_count + collect_count + comment_count) DESC " +
                    "LIMIT ?", userId, limit);

            for (Map<String, Object> row : rows) {
                CreatorDashboardVO.NoteRanking ranking = new CreatorDashboardVO.NoteRanking();
                ranking.setNoteId(((Number) row.get("id")).longValue());
                ranking.setTitle((String) row.get("title"));
                ranking.setViews(((Number) row.getOrDefault("view_count", 0)).longValue());
                ranking.setLikes(((Number) row.getOrDefault("like_count", 0)).longValue());
                ranking.setCollects(((Number) row.getOrDefault("collect_count", 0)).longValue());
                ranking.setComments(((Number) row.getOrDefault("comment_count", 0)).longValue());
                rankings.add(ranking);
            }
        } catch (Exception e) {
            log.warn("获取笔记排行失败, userId={}, limit={}, error={}", userId, limit, e.getMessage());
        }
        return rankings;
    }

    @Override
    public CreatorDashboardVO.FanProfile getFanProfile(Long userId) {
        CreatorDashboardVO.FanProfile fanProfile = new CreatorDashboardVO.FanProfile();
        Map<String, Integer> genderDist = new LinkedHashMap<>();
        genderDist.put("male", 0);
        genderDist.put("female", 0);
        genderDist.put("unknown", 0);
        fanProfile.setGenderDistribution(genderDist);

        List<Map<String, Object>> topRegions = new ArrayList<>();
        fanProfile.setTopRegions(topRegions);

        try {
            List<Map<String, Object>> genderRows = jdbcTemplate.queryForList(
                    "SELECT u.gender, COUNT(*) AS cnt " +
                    "FROM user u " +
                    "WHERE u.id IN (SELECT f.follower_id FROM follow f WHERE f.user_id = ?) " +
                    "GROUP BY u.gender", userId);

            for (Map<String, Object> row : genderRows) {
                String gender = (String) row.get("gender");
                int cnt = ((Number) row.get("cnt")).intValue();
                if ("male".equalsIgnoreCase(gender) || "1".equals(gender)) {
                    genderDist.put("male", genderDist.get("male") + cnt);
                } else if ("female".equalsIgnoreCase(gender) || "2".equals(gender)) {
                    genderDist.put("female", genderDist.get("female") + cnt);
                } else {
                    genderDist.put("unknown", genderDist.get("unknown") + cnt);
                }
            }
        } catch (Exception e) {
            log.warn("获取粉丝性别分布失败, userId={}, error={}", userId, e.getMessage());
        }

        try {
            List<Map<String, Object>> regionRows = jdbcTemplate.queryForList(
                    "SELECT u.region, COUNT(*) AS cnt " +
                    "FROM user u " +
                    "WHERE u.id IN (SELECT f.follower_id FROM follow f WHERE f.user_id = ?) " +
                    "AND u.region IS NOT NULL AND u.region != '' " +
                    "GROUP BY u.region ORDER BY cnt DESC LIMIT 10", userId);

            for (Map<String, Object> row : regionRows) {
                Map<String, Object> regionItem = new LinkedHashMap<>();
                regionItem.put("name", row.get("region"));
                regionItem.put("count", ((Number) row.get("cnt")).intValue());
                topRegions.add(regionItem);
            }
        } catch (Exception e) {
            log.warn("获取粉丝地域分布失败, userId={}, error={}", userId, e.getMessage());
        }

        return fanProfile;
    }

    @Override
    public Map<String, Object> getContentAnalytics(Long contentId) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT view_source, SUM(view_count) AS total_views, " +
                    "SUM(unique_view_count) AS total_unique_views, " +
                    "AVG(avg_read_duration) AS avg_read_duration " +
                    "FROM content_analytics WHERE content_id = ? GROUP BY view_source", contentId);

            Map<String, Long> sourceDistribution = new LinkedHashMap<>();
            long totalViews = 0;
            long totalUniqueViews = 0;
            int totalReadDuration = 0;
            int sourceCount = 0;

            for (Map<String, Object> row : rows) {
                String source = (String) row.get("view_source");
                long views = ((Number) row.getOrDefault("total_views", 0)).longValue();
                sourceDistribution.put(source, sourceDistribution.getOrDefault(source, 0L) + views);
                totalViews += views;
                totalUniqueViews += ((Number) row.getOrDefault("total_unique_views", 0)).longValue();
                Object avgDur = row.get("avg_read_duration");
                if (avgDur != null) {
                    totalReadDuration += ((Number) avgDur).intValue();
                    sourceCount++;
                }
            }

            result.put("contentId", contentId);
            result.put("totalViews", totalViews);
            result.put("totalUniqueViews", totalUniqueViews);
            result.put("avgReadDuration", sourceCount > 0 ? totalReadDuration / sourceCount : 0);
            result.put("sourceDistribution", sourceDistribution);
        } catch (Exception e) {
            log.warn("获取内容分析失败, contentId={}, error={}", contentId, e.getMessage());
            result.put("contentId", contentId);
            result.put("sourceDistribution", new LinkedHashMap<>());
        }

        return result;
    }

    @Override
    public Map<String, Object> getContentTrafficFunnel(Long contentId, String dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contentId", contentId);

        String startDate;
        String endDate;
        LocalDate today = LocalDate.now();
        switch (dateRange != null ? dateRange.toUpperCase() : "LAST_7_DAYS") {
            case "TODAY":
                startDate = today.format(ISO_DATE_FMT);
                endDate = startDate;
                break;
            case "LAST_30_DAYS":
                startDate = today.minusDays(29).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
            case "LAST_90_DAYS":
                startDate = today.minusDays(89).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
            case "LAST_7_DAYS":
            default:
                startDate = today.minusDays(6).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) AS date, " +
                    "SUM(impression_count) AS impression, " +
                    "SUM(click_count) AS click, " +
                    "SUM(read_count) AS read, " +
                    "SUM(interact_count) AS interact, " +
                    "SUM(share_count) AS share " +
                    "FROM traffic_funnel " +
                    "WHERE content_id = ? AND date >= ? AND date <= ? " +
                    "GROUP BY DATE(created_at) ORDER BY date",
                    contentId, startDate, endDate);

            long totalImpression = 0, totalClick = 0, totalRead = 0, totalInteract = 0, totalShare = 0;
            List<Map<String, Object>> dailyData = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                long impression = ((Number) row.getOrDefault("impression", 0)).longValue();
                long click = ((Number) row.getOrDefault("click", 0)).longValue();
                long read = ((Number) row.getOrDefault("read", 0)).longValue();
                long interact = ((Number) row.getOrDefault("interact", 0)).longValue();
                long share = ((Number) row.getOrDefault("share", 0)).longValue();

                totalImpression += impression;
                totalClick += click;
                totalRead += read;
                totalInteract += interact;
                totalShare += share;

                Map<String, Object> daily = new LinkedHashMap<>();
                daily.put("date", row.get("date"));
                daily.put("impression", impression);
                daily.put("click", click);
                daily.put("read", read);
                daily.put("interact", interact);
                daily.put("share", share);
                dailyData.add(daily);
            }

            Map<String, Object> funnel = new LinkedHashMap<>();
            funnel.put("impression", totalImpression);
            funnel.put("click", totalClick);
            funnel.put("read", totalRead);
            funnel.put("interact", totalInteract);
            funnel.put("share", totalShare);

            Map<String, Object> conversionRates = new LinkedHashMap<>();
            conversionRates.put("impressionToClick", totalImpression > 0 ?
                    new BigDecimal(totalClick * 100.0 / totalImpression).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            conversionRates.put("clickToRead", totalClick > 0 ?
                    new BigDecimal(totalRead * 100.0 / totalClick).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            conversionRates.put("readToInteract", totalRead > 0 ?
                    new BigDecimal(totalInteract * 100.0 / totalRead).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            conversionRates.put("interactToShare", totalInteract > 0 ?
                    new BigDecimal(totalShare * 100.0 / totalInteract).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

            result.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
            result.put("funnel", funnel);
            result.put("conversionRates", conversionRates);
            result.put("dailyData", dailyData);
        } catch (Exception e) {
            log.warn("获取内容转化漏斗失败, contentId={}, error={}", contentId, e.getMessage());
            result.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
            result.put("funnel", new LinkedHashMap<>());
            result.put("conversionRates", new LinkedHashMap<>());
            result.put("dailyData", new ArrayList<>());
        }

        return result;
    }

    @Override
    public Map<String, Object> getCreatorTrafficSources(Long creatorId, String dateRange) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("creatorId", creatorId);

        LocalDate today = LocalDate.now();
        String startDate;
        String endDate;
        switch (dateRange != null ? dateRange.toUpperCase() : "LAST_7_DAYS") {
            case "TODAY":
                startDate = today.format(ISO_DATE_FMT);
                endDate = startDate;
                break;
            case "LAST_30_DAYS":
                startDate = today.minusDays(29).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
            case "LAST_90_DAYS":
                startDate = today.minusDays(89).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
            case "LAST_7_DAYS":
            default:
                startDate = today.minusDays(6).format(ISO_DATE_FMT);
                endDate = today.format(ISO_DATE_FMT);
                break;
        }

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT ca.view_source, SUM(ca.view_count) AS total_views, " +
                    "SUM(ca.unique_view_count) AS total_unique_views, " +
                    "AVG(ca.avg_read_duration) AS avg_read_duration, " +
                    "SUM(ca.share_count) AS total_shares, " +
                    "SUM(ca.save_count) AS total_saves " +
                    "FROM content_analytics ca " +
                    "INNER JOIN note n ON ca.content_id = n.id " +
                    "WHERE n.user_id = ? AND ca.date >= ? AND ca.date <= ? " +
                    "GROUP BY ca.view_source " +
                    "ORDER BY total_views DESC", creatorId, startDate, endDate);

            long grandTotalViews = 0;
            for (Map<String, Object> row : rows) {
                grandTotalViews += ((Number) row.getOrDefault("total_views", 0)).longValue();
            }

            List<Map<String, Object>> sourceList = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> source = new LinkedHashMap<>();
                String viewSource = (String) row.get("view_source");
                long totalViews = ((Number) row.getOrDefault("total_views", 0)).longValue();
                source.put("source", viewSource);
                source.put("views", totalViews);
                source.put("uniqueViews", ((Number) row.getOrDefault("total_unique_views", 0)).longValue());
                source.put("avgReadDuration", row.get("avg_read_duration") != null ?
                        ((Number) row.get("avg_read_duration")).intValue() : 0);
                source.put("shares", ((Number) row.getOrDefault("total_shares", 0)).longValue());
                source.put("saves", ((Number) row.getOrDefault("total_saves", 0)).longValue());
                source.put("percentage", grandTotalViews > 0 ?
                        new BigDecimal(totalViews * 100.0 / grandTotalViews).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                sourceList.add(source);
            }

            result.put("dateRange", Map.of("startDate", startDate, "endDate", endDate));
            result.put("totalViews", grandTotalViews);
            result.put("sources", sourceList);
        } catch (Exception e) {
            log.warn("获取创作者流量来源失败, creatorId={}, error={}", creatorId, e.getMessage());
            result.put("totalViews", 0);
            result.put("sources", new ArrayList<>());
        }

        return result;
    }

    @Override
    public Map<String, Object> getCreatorContentDiagnosis(Long creatorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("creatorId", creatorId);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, title, view_count, like_count, comment_count, collect_count, share_count, " +
                    "created_at, status " +
                    "FROM note " +
                    "WHERE user_id = ? AND status = 1 AND deleted = 0 " +
                    "ORDER BY created_at DESC", creatorId);

            List<Map<String, Object>> contentList = new ArrayList<>();
            long totalViews = 0;
            long totalLikes = 0;
            long totalComments = 0;
            long totalCollects = 0;
            long totalShares = 0;

            for (Map<String, Object> row : rows) {
                long views = ((Number) row.getOrDefault("view_count", 0)).longValue();
                long likes = ((Number) row.getOrDefault("like_count", 0)).longValue();
                long comments = ((Number) row.getOrDefault("comment_count", 0)).longValue();
                long collects = ((Number) row.getOrDefault("collect_count", 0)).longValue();
                long shares = ((Number) row.getOrDefault("share_count", 0)).longValue();

                totalViews += views;
                totalLikes += likes;
                totalComments += comments;
                totalCollects += collects;
                totalShares += shares;

                Map<String, Object> content = new LinkedHashMap<>();
                content.put("contentId", row.get("id"));
                content.put("title", row.get("title"));
                content.put("views", views);
                content.put("likes", likes);
                content.put("comments", comments);
                content.put("collects", collects);
                content.put("shares", shares);
                content.put("createdAt", row.get("created_at"));
                content.put("interactRate", views > 0 ?
                        new BigDecimal((likes + comments + collects) * 100.0 / views).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
                contentList.add(content);
            }

            int contentCount = contentList.size();
            double avgViews = contentCount > 0 ? (double) totalViews / contentCount : 0;
            double avgLikes = contentCount > 0 ? (double) totalLikes / contentCount : 0;
            double avgInteractRate = contentCount > 0 ?
                    (totalViews > 0 ? (totalLikes + totalComments + totalCollects) * 100.0 / totalViews : 0) : 0;

            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("contentCount", contentCount);
            overview.put("totalViews", totalViews);
            overview.put("totalLikes", totalLikes);
            overview.put("totalComments", totalComments);
            overview.put("totalCollects", totalCollects);
            overview.put("totalShares", totalShares);
            overview.put("avgViews", Math.round(avgViews));
            overview.put("avgLikes", Math.round(avgLikes));
            overview.put("avgInteractRate", new BigDecimal(avgInteractRate).setScale(2, RoundingMode.HALF_UP));

            result.put("overview", overview);
            result.put("contentList", contentList);

            List<String> suggestions = new ArrayList<>();
            if (contentCount == 0) {
                suggestions.add("还没有发布内容，建议尽快发布第一篇笔记");
            } else {
                if (avgInteractRate < 5) {
                    suggestions.add("整体互动率偏低，建议优化内容质量和标题吸引力");
                }
                if (avgViews < 100) {
                    suggestions.add("平均浏览量较低，建议使用热门话题标签增加曝光");
                }

                contentList.stream()
                        .max(Comparator.comparingLong(c -> ((Number) c.get("views")).longValue()))
                        .ifPresent(bestContent -> {
                            suggestions.add("表现最好的内容: 「" + bestContent.get("title") + "」，浏览量 " + bestContent.get("views"));
                        });

                if (contentCount > 0 && contentCount < 5) {
                    suggestions.add("内容数量较少，建议保持稳定的发布频率(每周至少2-3篇)");
                }
            }

            result.put("suggestions", suggestions);
        } catch (Exception e) {
            log.warn("获取内容诊断失败, creatorId={}, error={}", creatorId, e.getMessage());
            result.put("overview", new LinkedHashMap<>());
            result.put("contentList", new ArrayList<>());
            result.put("suggestions", new ArrayList<>());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getCreatorDailyStats(Long creatorId, String startDate, String endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            List<CreatorDailyStats> statsList = creatorDailyStatsMapper.selectByCreatorIdAndDateRange(
                    creatorId, startDate, endDate);

            for (CreatorDailyStats stats : statsList) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", stats.getDate().format(ISO_DATE_FMT));
                item.put("newFollowers", stats.getNewFollowers() != null ? stats.getNewFollowers() : 0);
                item.put("unfollowers", stats.getUnfollowers() != null ? stats.getUnfollowers() : 0);
                item.put("totalFollowers", stats.getTotalFollowers() != null ? stats.getTotalFollowers() : 0);
                item.put("contentCount", stats.getContentCount() != null ? stats.getContentCount() : 0);
                item.put("totalViews", stats.getTotalViews() != null ? stats.getTotalViews() : 0);
                item.put("totalLikes", stats.getTotalLikes() != null ? stats.getTotalLikes() : 0);
                item.put("totalComments", stats.getTotalComments() != null ? stats.getTotalComments() : 0);
                item.put("totalShares", stats.getTotalShares() != null ? stats.getTotalShares() : 0);
                item.put("totalRevenue", stats.getTotalRevenue() != null ? stats.getTotalRevenue() : BigDecimal.ZERO);
                result.add(item);
            }
        } catch (Exception e) {
            log.warn("获取创作者每日统计失败, creatorId={}, error={}", creatorId, e.getMessage());
        }

        return result;
    }
}
