package com.langtou.message.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.message.dto.PushSettingsUpdateDTO;
import com.langtou.message.entity.PushSettings;
import com.langtou.message.service.PushSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 推送设置Controller
 */
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "推送设置", description = "推送设置接口")
    public class PushSettingsController {

    private final PushSettingsService pushSettingsService;

    /**
     * 获取推送设置
     * GET /api/v1/push/settings
     */
    @GetMapping("/settings")
    public Result<PushSettings> getSettings(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        PushSettings settings = pushSettingsService.getSettings(userId);
        return Result.success(settings);
    }

    /**
     * 更新推送设置
     * PUT /api/v1/push/settings
     */
    @PutMapping("/settings")
    public Result<PushSettings> updateSettings(
            @RequestBody PushSettingsUpdateDTO dto,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        PushSettings settings = new PushSettings();
        settings.setPrivateMessageEnabled(dto.getPrivateMessageEnabled());
        settings.setInteractionEnabled(dto.getInteractionEnabled());
        settings.setSystemEnabled(dto.getSystemEnabled());
        settings.setMarketingEnabled(dto.getMarketingEnabled());
        settings.setQuietHoursStart(dto.getQuietHoursStart());
        settings.setQuietHoursEnd(dto.getQuietHoursEnd());
        settings.setDailyLimit(dto.getDailyLimit());

        PushSettings updated = pushSettingsService.updateSettings(userId, settings);
        return Result.success("更新成功", updated);
    }
}
