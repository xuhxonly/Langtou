package com.langtou.common.client;

import com.langtou.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "langtou-content-service", path = "/api/v1", fallbackFactory = ContentClient.ContentClientFallbackFactory.class)
public interface ContentClient {

    @GetMapping("/notes/{noteId}")
    Result<Map<String, Object>> getNoteById(@PathVariable("noteId") Long noteId);

    @PostMapping("/notes/{noteId}/like-count/inc")
    Result<Void> incrementLikeCount(@PathVariable("noteId") Long noteId);

    @PostMapping("/notes/{noteId}/like-count/dec")
    Result<Void> decrementLikeCount(@PathVariable("noteId") Long noteId);

    @PostMapping("/notes/{noteId}/comment-count/inc")
    Result<Void> incrementCommentCount(@PathVariable("noteId") Long noteId);

    @PostMapping("/notes/{noteId}/collect-count/inc")
    Result<Void> incrementCollectCount(@PathVariable("noteId") Long noteId);

    @PostMapping("/notes/{noteId}/collect-count/dec")
    Result<Void> decrementCollectCount(@PathVariable("noteId") Long noteId);

    /**
     * 检查文本是否包含敏感词
     */
    @PostMapping("/sensitive-words/check")
    Result<Map<String, Object>> checkSensitiveWord(@RequestBody Map<String, String> params);

    @Slf4j
    @Component
    class ContentClientFallbackFactory implements FallbackFactory<ContentClient> {
        @Override
        public ContentClient create(Throwable cause) {
            return new ContentClient() {
                @Override
                public Result<Map<String, Object>> getNoteById(Long noteId) {
                    log.error("ContentClient.getNoteById 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Void> incrementLikeCount(Long noteId) {
                    log.error("ContentClient.incrementLikeCount 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Void> decrementLikeCount(Long noteId) {
                    log.error("ContentClient.decrementLikeCount 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Void> incrementCommentCount(Long noteId) {
                    log.error("ContentClient.incrementCommentCount 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Void> incrementCollectCount(Long noteId) {
                    log.error("ContentClient.incrementCollectCount 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Void> decrementCollectCount(Long noteId) {
                    log.error("ContentClient.decrementCollectCount 调用失败, noteId={}", noteId, cause);
                    return Result.error("内容服务不可用");
                }

                @Override
                public Result<Map<String, Object>> checkSensitiveWord(Map<String, String> params) {
                    log.error("ContentClient.checkSensitiveWord 调用失败", cause);
                    // fallback: 容忍敏感词服务不可用，默认不拦截
                    Map<String, Object> fallbackResult = new java.util.HashMap<>();
                    fallbackResult.put("containsSensitiveWord", false);
                    return Result.success(fallbackResult);
                }
            };
        }
    }
}
