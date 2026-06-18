package com.langtou.message.service;

import com.langtou.message.entity.PushSettings;

/**
 * 推送设置服务
 */
public interface PushSettingsService {

    /**
     * 获取用户推送设置，不存在则自动创建默认设置
     *
     * @param userId 用户ID
     * @return 推送设置
     */
    PushSettings getSettings(Long userId);

    /**
     * 更新用户推送设置
     *
     * @param userId   用户ID
     * @param settings 要更新的设置
     * @return 更新后的设置
     */
    PushSettings updateSettings(Long userId, PushSettings settings);
}
