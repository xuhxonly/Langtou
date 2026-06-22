package com.langtou.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.user.entity.TeenModeConfig;
import com.langtou.user.service.TeenModeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "青少年模式", description = "青少年模式管理")
@RestController
@RequestMapping("/api/v1/teen-mode")
@RequiredArgsConstructor
public class TeenModeController {

    private final TeenModeService teenModeService;

    @Operation(summary = "开启青少年模式")
    @PostMapping("/enable")
    public Result<Void> enableTeenMode(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                        @RequestBody Map<String, String> params) {
        String pin = params.get("pin");
        teenModeService.enableTeenMode(userId, pin);
        return Result.success("青少年模式已开启", null);
    }

    @Operation(summary = "关闭青少年模式（需PIN验证）")
    @PostMapping("/disable")
    public Result<Void> disableTeenMode(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                         @RequestBody Map<String, String> params) {
        String pin = params.get("pin");
        teenModeService.disableTeenMode(userId, pin);
        return Result.success("青少年模式已关闭", null);
    }

    @Operation(summary = "获取青少年模式配置")
    @GetMapping("/config")
    public Result<TeenModeConfig> getTeenModeConfig(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        TeenModeConfig config = teenModeService.getTeenModeConfig(userId);
        return Result.success(config);
    }

    @Operation(summary = "家长控制设置")
    @PutMapping("/parental-controls")
    public Result<TeenModeConfig> updateParentalControls(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestBody TeenModeConfig config) {
        TeenModeConfig updated = teenModeService.updateParentalControls(userId, config);
        return Result.success("家长控制设置已更新", updated);
    }

    @Operation(summary = "检查青少年模式状态（时长/夜间限制）")
    @GetMapping("/status")
    public Result<Map<String, Object>> getTeenModeStatus(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Map<String, Object> status = teenModeService.getTeenModeStatus(userId);
        return Result.success(status);
    }
}
