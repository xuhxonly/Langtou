package com.langtou.message.service.impl;

import com.langtou.message.entity.PushSettings;
import com.langtou.message.mapper.PushSettingsMapper;
import com.langtou.message.service.PushSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 推送设置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushSettingsServiceImpl implements PushSettingsService {

    private final PushSettingsMapper pushSettingsMapper;

    @Override
    public PushSettings getSettings(Long userId) {
        PushSettings settings = pushSettingsMapper.selectByUserId(userId);
        if (settings == null) {
            // 不存在则自动创建默认设置
            settings = createDefaultSettings(userId);
        }
        return settings;
    }

    @Override
    public PushSettings updateSettings(Long userId, PushSettings settings) {
        PushSettings existing = getSettings(userId);

        // 仅更新非null字段
        if (settings.getPrivateMessageEnabled() != null) {
            existing.setPrivateMessageEnabled(settings.getPrivateMessageEnabled());
        }
        if (settings.getInteractionEnabled() != null) {
            existing.setInteractionEnabled(settings.getInteractionEnabled());
        }
        if (settings.getSystemEnabled() != null) {
            existing.setSystemEnabled(settings.getSystemEnabled());
        }
        if (settings.getMarketingEnabled() != null) {
            existing.setMarketingEnabled(settings.getMarketingEnabled());
        }
        if (settings.getQuietHoursStart() != null) {
            existing.setQuietHoursStart(settings.getQuietHoursStart());
        }
        if (settings.getQuietHoursEnd() != null) {
            existing.setQuietHoursEnd(settings.getQuietHoursEnd());
        }
        if (settings.getDailyLimit() != null) {
            existing.setDailyLimit(settings.getDailyLimit());
        }

        pushSettingsMapper.updateById(existing);
        log.info("更新推送设置: userId={}", userId);
        return existing;
    }

    /**
     * 创建默认推送设置
     */
    private PushSettings createDefaultSettings(Long userId) {
        PushSettings settings = new PushSettings();
        settings.setUserId(userId);
        settings.setPrivateMessageEnabled(true);
        settings.setInteractionEnabled(true);
        settings.setSystemEnabled(true);
        settings.setMarketingEnabled(false);
        settings.setQuietHoursStart("23:00");
        settings.setQuietHoursEnd("08:00");
        settings.setDailyLimit(20);
        pushSettingsMapper.insert(settings);
        log.info("创建默认推送设置: userId={}", userId);
        return settings;
    }
}
