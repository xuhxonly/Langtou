package com.langtou.content.controller;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminAnalyticsController {
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        try {
            Long dau = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT user_id) FROM analytics_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            stats.put("dau", dau != null ? dau : 0);
        } catch (Exception e) { stats.put("dau", 0); }
        try {
            Long newUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            stats.put("newUsers", newUsers != null ? newUsers : 0);
        } catch (Exception e) { stats.put("newUsers", 0); }
        try {
            Long newNotes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM note WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            stats.put("newNotes", newNotes != null ? newNotes : 0);
        } catch (Exception e) { stats.put("newNotes", 0); }
        try {
            Long interactions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM analytics_event WHERE event_name IN ('note_like','note_collect','note_comment','note_share') AND created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            stats.put("interactions", interactions != null ? interactions : 0);
        } catch (Exception e) { stats.put("interactions", 0); }
        return Result.success(stats);
    }

    @GetMapping("/dashboard/trend")
    public Result<List<Map<String, Object>>> getDashboardTrend(@RequestParam(defaultValue = "7") int days) {
        String sql = "SELECT DATE(created_at) as date, event_name, COUNT(*) as count FROM analytics_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY DATE(created_at), event_name ORDER BY date";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, days);
        return Result.success(data);
    }

    @GetMapping("/users/trend")
    public Result<List<Map<String, Object>>> getUserTrend(@RequestParam(defaultValue = "30") int days) {
        String sql = "SELECT DATE(created_at) as date, COUNT(*) as count FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY DATE(created_at) ORDER BY date";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, days);
        return Result.success(data);
    }

    @GetMapping("/content/trend")
    public Result<List<Map<String, Object>>> getContentTrend(@RequestParam(defaultValue = "30") int days) {
        String sql = "SELECT DATE(created_at) as date, COUNT(*) as count FROM note WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY DATE(created_at) ORDER BY date";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, days);
        return Result.success(data);
    }

    @GetMapping("/interaction/trend")
    public Result<List<Map<String, Object>>> getInteractionTrend(@RequestParam(defaultValue = "14") int days) {
        String sql = "SELECT DATE(created_at) as date, event_name, COUNT(*) as count FROM analytics_event WHERE event_name IN ('note_like','note_collect','note_comment','note_share') AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY DATE(created_at), event_name ORDER BY date";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, days);
        return Result.success(data);
    }

    @GetMapping("/tags")
    public Result<List<Map<String, Object>>> getTagDistribution() {
        String sql = "SELECT t.name, COUNT(nt.note_id) as count FROM tag t LEFT JOIN note_tag nt ON t.id = nt.tag_id GROUP BY t.id, t.name ORDER BY count DESC LIMIT 20";
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);
        return Result.success(data);
    }

    // ==================== 增强管理分析接口 ====================

    /**
     * 平台总览（DAU/MAU/内容数/互动数/GMV）
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        try {
            Long dau = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM analytics_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            overview.put("dau", dau != null ? dau : 0);
        } catch (Exception e) { overview.put("dau", 0); }

        try {
            Long mau = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM analytics_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", Long.class);
            overview.put("mau", mau != null ? mau : 0);
        } catch (Exception e) { overview.put("mau", 0); }

        try {
            Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Long.class);
            overview.put("totalUsers", totalUsers != null ? totalUsers : 0);
        } catch (Exception e) { overview.put("totalUsers", 0); }

        try {
            Long totalContent = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM note WHERE deleted = 0", Long.class);
            overview.put("totalContent", totalContent != null ? totalContent : 0);
        } catch (Exception e) { overview.put("totalContent", 0); }

        try {
            Long totalInteractions = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM analytics_event WHERE event_name IN ('note_like','note_collect','note_comment','note_share')", Long.class);
            overview.put("totalInteractions", totalInteractions != null ? totalInteractions : 0);
        } catch (Exception e) { overview.put("totalInteractions", 0); }

        try {
            BigDecimal gmv = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(amount), 0) FROM creator_wallet", BigDecimal.class);
            overview.put("gmv", gmv != null ? gmv : BigDecimal.ZERO);
        } catch (Exception e) { overview.put("gmv", BigDecimal.ZERO); }

        try {
            Long todayNewUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            overview.put("todayNewUsers", todayNewUsers != null ? todayNewUsers : 0);
        } catch (Exception e) { overview.put("todayNewUsers", 0); }

        try {
            Long todayNewContent = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM note WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY) AND deleted = 0", Long.class);
            overview.put("todayNewContent", todayNewContent != null ? todayNewContent : 0);
        } catch (Exception e) { overview.put("todayNewContent", 0); }

        return Result.success(overview);
    }

    /**
     * 平台趋势（日/周/月）
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getPlatformTrend(
            @RequestParam(defaultValue = "daily") String granularity,
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        String intervalSql;
        switch (granularity.toLowerCase()) {
            case "weekly":
                intervalSql = "INTERVAL 7 DAY";
                break;
            case "monthly":
                intervalSql = "INTERVAL 30 DAY";
                break;
            default:
                intervalSql = "INTERVAL 1 DAY";
                break;
        }

        try {
            List<Map<String, Object>> userTrend = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) as date, COUNT(*) as count " +
                    "FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY date", days);
            result.put("userGrowth", userTrend);
        } catch (Exception e) {
            result.put("userGrowth", new ArrayList<>());
        }

        try {
            List<Map<String, Object>> contentTrend = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) as date, COUNT(*) as count " +
                    "FROM note WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) AND deleted = 0 " +
                    "GROUP BY DATE(created_at) ORDER BY date", days);
            result.put("contentGrowth", contentTrend);
        } catch (Exception e) {
            result.put("contentGrowth", new ArrayList<>());
        }

        try {
            List<Map<String, Object>> interactionTrend = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) as date, COUNT(*) as count " +
                    "FROM analytics_event " +
                    "WHERE event_name IN ('note_like','note_collect','note_comment','note_share') " +
                    "AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY date", days);
            result.put("interactionTrend", interactionTrend);
        } catch (Exception e) {
            result.put("interactionTrend", new ArrayList<>());
        }

        result.put("granularity", granularity);
        result.put("days", days);

        return Result.success(result);
    }

    /**
     * 创作者排行
     */
    @GetMapping("/top-creators")
    public Result<List<Map<String, Object>>> getTopCreators(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "views") String sortBy) {
        String orderBy;
        switch (sortBy.toLowerCase()) {
            case "likes":
                orderBy = "total_likes DESC";
                break;
            case "followers":
                orderBy = "followers_count DESC";
                break;
            case "content":
                orderBy = "content_count DESC";
                break;
            default:
                orderBy = "total_views DESC";
                break;
        }

        try {
            String sql = "SELECT n.user_id AS creatorId, u.username, u.avatar, " +
                    "COUNT(n.id) AS content_count, " +
                    "COALESCE(SUM(n.view_count), 0) AS total_views, " +
                    "COALESCE(SUM(n.like_count), 0) AS total_likes, " +
                    "COALESCE(SUM(n.comment_count), 0) AS total_comments, " +
                    "(SELECT COUNT(*) FROM follow f WHERE f.user_id = n.user_id) AS followers_count " +
                    "FROM note n LEFT JOIN user u ON n.user_id = u.id " +
                    "WHERE n.status = 1 AND n.deleted = 0 " +
                    "GROUP BY n.user_id, u.username, u.avatar " +
                    "ORDER BY " + orderBy + " LIMIT ?";
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, limit);
            return Result.success(data);
        } catch (Exception e) {
            log.warn("获取创作者排行失败, error={}", e.getMessage());
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 内容排行
     */
    @GetMapping("/top-content")
    public Result<List<Map<String, Object>>> getTopContent(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "views") String sortBy) {
        String orderBy;
        switch (sortBy.toLowerCase()) {
            case "likes":
                orderBy = "like_count DESC";
                break;
            case "comments":
                orderBy = "comment_count DESC";
                break;
            case "collects":
                orderBy = "collect_count DESC";
                break;
            case "shares":
                orderBy = "share_count DESC";
                break;
            default:
                orderBy = "view_count DESC";
                break;
        }

        try {
            String sql = "SELECT n.id, n.title, n.user_id AS creatorId, u.username AS creatorName, " +
                    "n.view_count, n.like_count, n.comment_count, n.collect_count, n.share_count, " +
                    "n.created_at " +
                    "FROM note n LEFT JOIN user u ON n.user_id = u.id " +
                    "WHERE n.status = 1 AND n.deleted = 0 " +
                    "ORDER BY " + orderBy + " LIMIT ?";
            List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, limit);
            return Result.success(data);
        } catch (Exception e) {
            log.warn("获取内容排行失败, error={}", e.getMessage());
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 用户增长分析
     */
    @GetMapping("/user-growth")
    public Result<Map<String, Object>> getUserGrowth(
            @RequestParam(defaultValue = "30") int days) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Long.class);
            result.put("totalUsers", totalUsers != null ? totalUsers : 0);
        } catch (Exception e) { result.put("totalUsers", 0); }

        try {
            Long activeUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM analytics_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", Long.class);
            result.put("activeUsers", activeUsers != null ? activeUsers : 0);
        } catch (Exception e) { result.put("activeUsers", 0); }

        try {
            Long newUsersToday = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)", Long.class);
            result.put("newUsersToday", newUsersToday != null ? newUsersToday : 0);
        } catch (Exception e) { result.put("newUsersToday", 0); }

        try {
            Long newUsersWeek = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)", Long.class);
            result.put("newUsersWeek", newUsersWeek != null ? newUsersWeek : 0);
        } catch (Exception e) { result.put("newUsersWeek", 0); }

        try {
            Long newUsersMonth = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", Long.class);
            result.put("newUsersMonth", newUsersMonth != null ? newUsersMonth : 0);
        } catch (Exception e) { result.put("newUsersMonth", 0); }

        try {
            List<Map<String, Object>> dailyGrowth = jdbcTemplate.queryForList(
                    "SELECT DATE(created_at) as date, COUNT(*) as count " +
                    "FROM user WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "GROUP BY DATE(created_at) ORDER BY date", days);
            result.put("dailyGrowth", dailyGrowth);
        } catch (Exception e) {
            result.put("dailyGrowth", new ArrayList<>());
        }

        try {
            // 留存率计算: 7日留存
            Long day0Users = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE created_at = DATE_SUB(CURDATE(), INTERVAL 7 DAY)", Long.class);
            Long retainedUsers = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT user_id) FROM analytics_event " +
                    "WHERE user_id IN (SELECT id FROM user WHERE created_at = DATE_SUB(CURDATE(), INTERVAL 7 DAY)) " +
                    "AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)", Long.class);
            BigDecimal retentionRate = (day0Users != null && day0Users > 0 && retainedUsers != null) ?
                    new BigDecimal(retainedUsers * 100.0 / day0Users).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            result.put("retentionRate7d", retentionRate);
        } catch (Exception e) {
            result.put("retentionRate7d", BigDecimal.ZERO);
        }

        return Result.success(result);
    }
}
