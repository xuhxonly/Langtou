package com.langtou.message.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.message.dto.DeviceTokenRegisterDTO;
import com.langtou.message.dto.DeviceTokenUnregisterDTO;
import com.langtou.message.entity.PushDeviceToken;
import com.langtou.message.service.PushDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推送设备管理Controller
 */
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "推送设备", description = "推送设备注册接口")
    public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    /**
     * 注册设备Token
     * POST /api/v1/push/token
     */
    @PostMapping("/token")
    public Result<PushDeviceToken> registerToken(
            @Valid @RequestBody DeviceTokenRegisterDTO dto,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        PushDeviceToken token = pushDeviceService.registerDevice(
                userId,
                dto.getDeviceType(),
                dto.getDeviceToken(),
                dto.getAppVersion(),
                dto.getOsVersion()
        );
        return Result.success("注册成功", token);
    }

    /**
     * 注销设备Token
     * DELETE /api/v1/push/token
     */
    @DeleteMapping("/token")
    public Result<Void> unregisterToken(
            @Valid @RequestBody DeviceTokenUnregisterDTO dto,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        pushDeviceService.unregisterDevice(userId, dto.getDeviceToken());
        return Result.success("注销成功");
    }

    /**
     * 获取用户设备列表
     * GET /api/v1/push/devices
     */
    @GetMapping("/devices")
    public Result<List<PushDeviceToken>> getUserDevices(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<PushDeviceToken> devices = pushDeviceService.getUserDevices(userId);
        return Result.success(devices);
    }
}
