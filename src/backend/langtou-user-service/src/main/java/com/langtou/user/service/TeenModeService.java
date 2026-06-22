package com.langtou.user.service;

import com.langtou.user.entity.TeenModeConfig;

import java.util.Map;

/**
 * 青少年模式服务
 */
public interface TeenModeService {

    /**
     * 开启青少年模式
     */
    void enableTeenMode(Long userId, String pin);

    /**
     * 关闭青少年模式（需PIN验证）
     */
    void disableTeenMode(Long userId, String pin);

    /**
     * 检查并执行使用时长限制（40分钟/天）
     * @return Map包含 allowed(是否允许继续使用) 和 remainingSeconds(剩余秒数)
     */
    Map<String, Object> checkAndEnforceUsageLimit(Long userId);

    /**
     * 检查夜间限制（22:00-6:00禁用）
     * @return Map包含 restricted(是否受限) 和 message
     */
    Map<String, Object> checkNightRestriction();

    /**
     * 内容分级过滤：判断某内容是否允许该青少年用户查看
     */
    boolean isContentAllowedForTeen(String ageRating, int userAge);

    /**
     * 获取青少年模式配置
     */
    TeenModeConfig getTeenModeConfig(Long userId);

    /**
     * 更新家长控制设置
     */
    TeenModeConfig updateParentalControls(Long userId, TeenModeConfig config);

    /**
     * 记录使用时长
     */
    void recordUsage(Long userId, int seconds);

    /**
     * 获取青少年模式状态（时长/夜间限制）
     */
    Map<String, Object> getTeenModeStatus(Long userId);
}
