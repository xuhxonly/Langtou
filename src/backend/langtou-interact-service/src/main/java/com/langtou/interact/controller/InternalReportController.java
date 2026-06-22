package com.langtou.interact.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.Result;
import com.langtou.interact.entity.Report;
import com.langtou.interact.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 举报内部接口（供其他微服务通过 Feign 调用）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal/reports")
@RequiredArgsConstructor
@Tag(name = "内部举报服务", description = "举报内部接口")
    public class InternalReportController {

    private final ReportService reportService;

    @GetMapping
    public Result<Map<String, Object>> getReportList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        Page<Report> reportPage = reportService.getReportList(page, size, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", reportPage.getRecords());
        data.put("total", reportPage.getTotal());
        data.put("current", reportPage.getCurrent());
        data.put("size", reportPage.getSize());
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getReportDetail(@PathVariable Long id) {
        Report report = reportService.getReportDetail(id);
        Map<String, Object> data = new HashMap<>();
        data.put("id", report.getId());
        data.put("reporterId", report.getReporterId());
        data.put("noteId", report.getNoteId());
        data.put("reason", report.getReason());
        data.put("reportType", report.getReportType());
        data.put("status", report.getStatus());
        data.put("createdAt", report.getCreatedAt());
        data.put("updatedAt", report.getUpdatedAt());
        return Result.success(data);
    }

    @PutMapping("/{id}/handle")
    public Result<Void> handleReport(@PathVariable Long id,
                                     @RequestBody Map<String, Object> params) {
        String handleResult = params.get("handleResult") != null ? params.get("handleResult").toString() : "";
        String action = params.get("action") != null ? params.get("action").toString() : "";
        reportService.handleReport(id, handleResult, action);
        return Result.success("处理成功");
    }
}
