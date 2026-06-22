package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.client.InteractClient;
import com.langtou.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "举报管理", description = "后台举报处理管理")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final InteractClient interactClient;

    @Operation(summary = "举报列表（分页、按状态筛选）")
    @GetMapping
    public Result<Map<String, Object>> getReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        return interactClient.getReportList(page, size, status);
    }

    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getReportDetail(@PathVariable Long id) {
        return interactClient.getReportDetail(id);
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @RequestBody Map<String, Object> params) {
        String handleResult = params.get("handleResult") != null ? params.get("handleResult").toString() : "";
        String action = params.get("action") != null ? params.get("action").toString() : "";
        log.info("管理员处理举报: reportId={}, action={}, handleResult={}", id, action, handleResult);
        return interactClient.handleReport(id, params);
    }
}
