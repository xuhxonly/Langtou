package com.langtou.content.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.content.entity.ContentSimilarity;
import com.langtou.content.entity.DeviceFingerprint;
import com.langtou.content.entity.FraudReport;
import com.langtou.content.service.AntiFraudService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 反作弊用户端接口
 *
 * 提供设备指纹上报、作弊举报、设备状态查询等功能。
 */
@Slf4j
@Tag(name = "反作弊", description = "反作弊用户端接口")
@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class AntiFraudController {

    private final AntiFraudService antiFraudService;

    @Operation(summary = "上报设备指纹")
    @PostMapping("/device-fingerprint")
    public Result<DeviceFingerprint> reportDeviceFingerprint(
            @RequestBody Map<String, String> params,
            HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.REQUEST_USER_ID);
        Long userId = (userIdStr != null && !userIdStr.isEmpty()) ? Long.parseLong(userIdStr) : null;

        String deviceId = params.get("deviceId");
        String deviceBrand = params.get("deviceBrand");
        String deviceModel = params.get("deviceModel");
        String osType = params.get("osType");
        String osVersion = params.get("osVersion");
        String appVersion = params.get("appVersion");
        String ipAddress = request.getRemoteAddr();

        DeviceFingerprint fingerprint = antiFraudService.recordDeviceFingerprint(
                userId, deviceId, deviceBrand, deviceModel, osType, osVersion, appVersion, ipAddress);
        return Result.success(fingerprint);
    }

    @Operation(summary = "举报作弊")
    @PostMapping("/report")
    public Result<FraudReport> reportFraud(
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String userIdStr = request.getHeader(CommonConstants.REQUEST_USER_ID);
        if (userIdStr == null || userIdStr.isEmpty()) {
            return Result.error(401, "未授权，请先登录");
        }
        Long userId = Long.parseLong(userIdStr);

        String fraudType = (String) params.get("fraudType");
        String description = (String) params.get("description");
        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) params.get("evidence");

        FraudReport report = antiFraudService.reportFraud(userId, fraudType, description, evidence);
        return Result.success(report);
    }

    @Operation(summary = "检查设备状态")
    @GetMapping("/device/{deviceId}/status")
    public Result<Map<String, Object>> checkDeviceStatus(@PathVariable String deviceId) {
        boolean blocked = antiFraudService.isDeviceBlocked(deviceId);
        Map<String, Object> status = Map.of(
                "deviceId", deviceId,
                "blocked", blocked
        );
        return Result.success(status);
    }
}
