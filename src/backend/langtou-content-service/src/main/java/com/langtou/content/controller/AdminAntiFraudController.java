package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
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
 * 反作弊管理后台接口
 *
 * 提供举报管理、设备管理、反作弊统计等管理员功能。
 */
@Slf4j
@Tag(name = "反作弊管理", description = "反作弊后台管理接口")
@RestController
@RequestMapping("/api/v1/admin/fraud")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminAntiFraudController {

    private final AntiFraudService antiFraudService;

    @Operation(summary = "举报列表")
    @GetMapping("/reports")
    public Result<List<FraudReport>> getFraudReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        List<FraudReport> reports = antiFraudService.getFraudReports(page, size, status);
        return Result.success(reports);
    }

    @Operation(summary = "处理举报")
    @PutMapping("/reports/{id}/process")
    public Result<Void> processFraudReport(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params,
            HttpServletRequest request) {
        String status = (String) params.get("status");
        String processorIdStr = request.getHeader(CommonConstants.REQUEST_USER_ID);
        Long processorId = (processorIdStr != null && !processorIdStr.isEmpty()) ? Long.parseLong(processorIdStr) : null;

        antiFraudService.processFraudReport(id, status, processorId);
        return Result.success();
    }

    @Operation(summary = "设备列表")
    @GetMapping("/devices")
    public Result<List<DeviceFingerprint>> getDeviceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<DeviceFingerprint> devices = antiFraudService.getDeviceList(page, size);
        return Result.success(devices);
    }

    @Operation(summary = "封禁设备")
    @PutMapping("/devices/{deviceId}/block")
    public Result<Void> blockDevice(@PathVariable String deviceId) {
        antiFraudService.blockDevice(deviceId);
        return Result.success();
    }

    @Operation(summary = "反作弊统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = antiFraudService.getStatistics();
        return Result.success(stats);
    }
}
