package com.langtou.common.client;

import com.langtou.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FeignClient(name = "langtou-interact-service", path = "/api/v1", fallbackFactory = InteractClient.InteractClientFallbackFactory.class)
public interface InteractClient {

    @GetMapping("/notes/{noteId}/like/status")
    Result<Map<String, Object>> likeStatus(@PathVariable("noteId") Long noteId,
                                           @RequestHeader("userId") Long userId);

    @GetMapping("/notes/{noteId}/collect/status")
    Result<Boolean> collectStatus(@PathVariable("noteId") Long noteId,
                                  @RequestHeader("userId") Long userId);

    // ========== 内部接口（供 AdminReportController 调用） ==========

    @GetMapping("/internal/reports")
    Result<Map<String, Object>> getReportList(@RequestParam("page") int page,
                                              @RequestParam("size") int size,
                                              @RequestParam(value = "status", required = false) Integer status);

    @GetMapping("/internal/reports/{id}")
    Result<Map<String, Object>> getReportDetail(@PathVariable("id") Long id);

    @PutMapping("/internal/reports/{id}/handle")
    Result<Void> handleReport(@PathVariable("id") Long id,
                              @RequestBody Map<String, Object> params);

    @Slf4j
    @Component
    class InteractClientFallbackFactory implements FallbackFactory<InteractClient> {
        @Override
        public InteractClient create(Throwable cause) {
            return new InteractClient() {
                @Override
                public Result<Map<String, Object>> likeStatus(Long noteId, Long userId) {
                    log.error("InteractClient.likeStatus 调用失败, noteId={}, userId={}", noteId, userId, cause);
                    Map<String, Object> fallbackData = new HashMap<>();
                    fallbackData.put("liked", false);
                    fallbackData.put("likeCount", 0);
                    return Result.success(fallbackData);
                }

                @Override
                public Result<Boolean> collectStatus(Long noteId, Long userId) {
                    log.error("InteractClient.collectStatus 调用失败, noteId={}, userId={}", noteId, userId, cause);
                    return Result.success(false);
                }

                @Override
                public Result<Map<String, Object>> getReportList(int page, int size, Integer status) {
                    log.error("InteractClient.getReportList 调用失败, page={}, size={}, status={}", page, size, status, cause);
                    return Result.error("互动服务不可用");
                }

                @Override
                public Result<Map<String, Object>> getReportDetail(Long id) {
                    log.error("InteractClient.getReportDetail 调用失败, id={}", id, cause);
                    return Result.error("互动服务不可用");
                }

                @Override
                public Result<Void> handleReport(Long id, Map<String, Object> params) {
                    log.error("InteractClient.handleReport 调用失败, id={}", id, cause);
                    return Result.error("互动服务不可用");
                }
            };
        }
    }
}
