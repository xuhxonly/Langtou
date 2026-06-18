package com.langtou.user.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.user.entity.TeenModeConfig;
import com.langtou.user.entity.TeenModeLog;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.TeenModeMapper;
import com.langtou.user.mapper.UserMapper;
import com.langtou.user.service.TeenModeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 青少年模式服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeenModeServiceImpl implements TeenModeService {

    private final UserMapper userMapper;
    private final TeenModeMapper teenModeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 每日使用时长限制：40分钟 = 2400秒
     */
    private static final int DAILY_USAGE_LIMIT_SECONDS = 40 * 60;

    /**
     * 夜间禁用开始时间：22:00
     */
    private static final int NIGHT_RESTRICTION_START = 22;

    /**
     * 夜间禁用结束时间：6:00
     */
    private static final int NIGHT_RESTRICTION_END = 6;

    /**
     * Redis key: 青少年模式配置缓存
     */
    private static final String TEEN_MODE_CONFIG_KEY = "teen:mode:config:";

    /**
     * Redis key: 当日使用时长计数器
     */
    private static final String TEEN_MODE_USAGE_KEY = "teen:mode:usage:";

    @Override
    public void enableTeenMode(Long userId, String pin) {
        if (pin == null || pin.length() < 4) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "PIN码长度不能少于4位");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 加密存储PIN码
        user.setTeenModePin(passwordEncoder.encode(pin));
        user.setTeenModeEnabled(1);
        user.setDailyUsageSeconds(0);
        user.setLastUsageDate(null);
        userMapper.updateById(user);

        // 清除Redis缓存
        stringRedisTemplate.delete(TEEN_MODE_CONFIG_KEY + userId);

        log.info("青少年模式已开启: userId={}", userId);
    }

    @Override
    public void disableTeenMode(Long userId, String pin) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getTeenModeEnabled() == null || user.getTeenModeEnabled() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "青少年模式未开启");
        }

        // 验证PIN码
        if (user.getTeenModePin() == null || !passwordEncoder.matches(pin, user.getTeenModePin())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "PIN码错误");
        }

        user.setTeenModeEnabled(0);
        user.setTeenModePin(null);
        user.setDailyUsageSeconds(0);
        user.setLastUsageDate(null);
        userMapper.updateById(user);

        // 清除Redis缓存
        stringRedisTemplate.delete(TEEN_MODE_CONFIG_KEY + userId);
        stringRedisTemplate.delete(TEEN_MODE_USAGE_KEY + userId);

        log.info("青少年模式已关闭: userId={}", userId);
    }

    @Override
    public Map<String, Object> checkAndEnforceUsageLimit(Long userId) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null || user.getTeenModeEnabled() == null || user.getTeenModeEnabled() != 1) {
            result.put("allowed", true);
            result.put("remainingSeconds", DAILY_USAGE_LIMIT_SECONDS);
            return result;
        }

        // 检查日期是否需要重置
        String today = LocalDate.now().toString();
        if (user.getLastUsageDate() == null || !today.equals(user.getLastUsageDate())) {
            // 新的一天，重置使用时长
            user.setDailyUsageSeconds(0);
            user.setLastUsageDate(today);
            userMapper.updateById(user);
            // 清除Redis计数器
            stringRedisTemplate.delete(TEEN_MODE_USAGE_KEY + userId);
        }

        int usedSeconds = user.getDailyUsageSeconds() != null ? user.getDailyUsageSeconds() : 0;
        int remainingSeconds = Math.max(0, DAILY_USAGE_LIMIT_SECONDS - usedSeconds);

        result.put("allowed", usedSeconds < DAILY_USAGE_LIMIT_SECONDS);
        result.put("remainingSeconds", remainingSeconds);
        result.put("usedSeconds", usedSeconds);
        result.put("dailyLimitSeconds", DAILY_USAGE_LIMIT_SECONDS);

        if (usedSeconds >= DAILY_USAGE_LIMIT_SECONDS) {
            log.info("青少年模式: 用户今日使用时长已达上限: userId={}, used={}s", userId, usedSeconds);
        }

        return result;
    }

    @Override
    public Map<String, Object> checkNightRestriction() {
        Map<String, Object> result = new HashMap<>();

        int currentHour = LocalTime.now().getHour();
        boolean restricted = currentHour >= NIGHT_RESTRICTION_START || currentHour < NIGHT_RESTRICTION_END;

        result.put("restricted", restricted);
        result.put("currentHour", currentHour);
        result.put("nightStart", NIGHT_RESTRICTION_START);
        result.put("nightEnd", NIGHT_RESTRICTION_END);

        if (restricted) {
            result.put("message", "当前为夜间休息时段（22:00-6:00），青少年模式已自动限制使用");
        } else {
            result.put("message", "当前不在夜间限制时段");
        }

        return result;
    }

    @Override
    public boolean isContentAllowedForTeen(String ageRating, int userAge) {
        if (ageRating == null) {
            return true;
        }

        return switch (ageRating.toUpperCase()) {
            case "ALL" -> true;
            case "7+" -> userAge >= 7;
            case "12+" -> userAge >= 12;
            case "18+" -> userAge >= 18;
            default -> true;
        };
    }

    @Override
    public TeenModeConfig getTeenModeConfig(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        TeenModeConfig config = new TeenModeConfig();
        config.setUserId(user.getId());
        config.setAgeVerified(user.getAgeVerified());
        config.setVerifiedAge(user.getVerifiedAge());
        config.setTeenModeEnabled(user.getTeenModeEnabled());
        config.setDailyUsageLimit(DAILY_USAGE_LIMIT_SECONDS);
        config.setDailyUsageSeconds(user.getDailyUsageSeconds());
        config.setLastUsageDate(user.getLastUsageDate());
        config.setNightRestrictionStart(NIGHT_RESTRICTION_START);
        config.setNightRestrictionEnd(NIGHT_RESTRICTION_END);
        config.setNightRestrictionEnabled(1);
        config.setContentRatingLimit("12+");

        return config;
    }

    @Override
    public TeenModeConfig updateParentalControls(Long userId, TeenModeConfig config) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (user.getTeenModeEnabled() == null || user.getTeenModeEnabled() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先开启青少年模式");
        }

        // 更新年龄验证信息
        if (config.getAgeVerified() != null) {
            user.setAgeVerified(config.getAgeVerified());
        }
        if (config.getVerifiedAge() != null) {
            user.setVerifiedAge(config.getVerifiedAge());
        }

        // 如果提供了新PIN码，则更新
        if (config.getTeenModePin() != null && config.getTeenModePin().length() >= 4) {
            user.setTeenModePin(passwordEncoder.encode(config.getTeenModePin()));
        }

        userMapper.updateById(user);

        // 清除Redis缓存
        stringRedisTemplate.delete(TEEN_MODE_CONFIG_KEY + userId);

        log.info("家长控制设置已更新: userId={}", userId);
        return getTeenModeConfig(userId);
    }

    @Override
    public void recordUsage(Long userId, int seconds) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getTeenModeEnabled() == null || user.getTeenModeEnabled() != 1) {
            return;
        }

        String today = LocalDate.now().toString();

        // 检查日期是否需要重置
        if (user.getLastUsageDate() == null || !today.equals(user.getLastUsageDate())) {
            user.setDailyUsageSeconds(0);
            user.setLastUsageDate(today);
        }

        // 累加使用时长
        int newTotal = (user.getDailyUsageSeconds() != null ? user.getDailyUsageSeconds() : 0) + seconds;
        user.setDailyUsageSeconds(newTotal);
        userMapper.updateById(user);

        // 记录到日志表
        try {
            teenModeMapper.updateDailyUsage(userId, today, seconds);
        } catch (Exception e) {
            log.error("记录青少年模式使用日志失败: userId={}, error={}", userId, e.getMessage());
        }

        log.debug("青少年模式使用时长记录: userId={}, seconds={}, total={}", userId, seconds, newTotal);
    }

    @Override
    public Map<String, Object> getTeenModeStatus(Long userId) {
        Map<String, Object> status = new HashMap<>();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        boolean teenModeEnabled = user.getTeenModeEnabled() != null && user.getTeenModeEnabled() == 1;
        status.put("teenModeEnabled", teenModeEnabled);

        if (teenModeEnabled) {
            // 使用时长限制检查
            Map<String, Object> usageLimit = checkAndEnforceUsageLimit(userId);
            status.put("usageLimit", usageLimit);

            // 夜间限制检查
            Map<String, Object> nightRestriction = checkNightRestriction();
            status.put("nightRestriction", nightRestriction);

            // 综合判断是否可以继续使用
            boolean canUse = (boolean) usageLimit.get("allowed") && !(boolean) nightRestriction.get("restricted");
            status.put("canUse", canUse);
        }

        return status;
    }
}
