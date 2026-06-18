package com.langtou.content.controller;

import com.langtou.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/notification-templates")
    public Result<List<Map<String, Object>>> getNotificationTemplates() {
        try {
            List<Map<String, Object>> templates = jdbcTemplate.queryForList("SELECT * FROM notification_template ORDER BY id");
            return Result.success(templates);
        } catch (Exception e) {
            // 表可能不存在，返回默认模板
            return Result.success(List.of());
        }
    }

    @GetMapping("/points-rules")
    public Result<List<Map<String, Object>>> getPointsRules() {
        try {
            List<Map<String, Object>> rules = jdbcTemplate.queryForList("SELECT * FROM points_rule ORDER BY id");
            return Result.success(rules);
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    @PostMapping("/points-rules")
    public Result<Void> savePointsRules(@RequestBody List<Map<String, Object>> rules) {
        log.info("保存积分规则: count={}", rules.size());
        // 真实持久化：先清空再批量插入（简单实现，生产环境建议用 UPSERT）
        try {
            jdbcTemplate.update("DELETE FROM points_rule");
            if (!rules.isEmpty()) {
                String sql = "INSERT INTO points_rule (id, action_type, points, description, created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
                jdbcTemplate.batchUpdate(sql, rules, rules.size(), (ps, rule) -> {
                    Object idObj = rule.get("id");
                    ps.setObject(1, idObj != null ? ((Number) idObj).longValue() : null);
                    ps.setString(2, (String) rule.get("action_type"));
                    Object pointsObj = rule.get("points");
                    ps.setInt(3, pointsObj != null ? ((Number) pointsObj).intValue() : 0);
                    ps.setString(4, (String) rule.get("description"));
                });
            }
            return Result.success("保存成功");
        } catch (Exception e) {
            log.error("保存积分规则失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/level-rules")
    public Result<List<Map<String, Object>>> getLevelRules() {
        try {
            List<Map<String, Object>> rules = jdbcTemplate.queryForList("SELECT * FROM user_level ORDER BY level");
            return Result.success(rules);
        } catch (Exception e) {
            return Result.success(List.of());
        }
    }

    @PostMapping("/level-rules")
    public Result<Void> saveLevelRules(@RequestBody List<Map<String, Object>> rules) {
        log.info("保存等级规则: count={}", rules.size());
        // 真实持久化：先清空再批量插入（简单实现，生产环境建议用 UPSERT）
        try {
            jdbcTemplate.update("DELETE FROM user_level_rule");
            if (!rules.isEmpty()) {
                String sql = "INSERT INTO user_level_rule (id, level, min_points, max_points, privilege, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NOW(), NOW())";
                jdbcTemplate.batchUpdate(sql, rules, rules.size(), (ps, rule) -> {
                    Object idObj = rule.get("id");
                    ps.setObject(1, idObj != null ? ((Number) idObj).longValue() : null);
                    Object levelObj = rule.get("level");
                    ps.setInt(2, levelObj != null ? ((Number) levelObj).intValue() : 0);
                    Object minObj = rule.get("min_points");
                    ps.setInt(3, minObj != null ? ((Number) minObj).intValue() : 0);
                    Object maxObj = rule.get("max_points");
                    ps.setInt(4, maxObj != null ? ((Number) maxObj).intValue() : 0);
                    ps.setString(5, (String) rule.get("privilege"));
                });
            }
            return Result.success("保存成功");
        } catch (Exception e) {
            log.error("保存等级规则失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }
}
