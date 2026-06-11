package com.langtou.common.client;

import com.langtou.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "langtou-interact-service", path = "/api/v1")
public interface InteractClient {

    @GetMapping("/notes/{noteId}/like/status")
    Result<Map<String, Object>> likeStatus(@PathVariable("noteId") Long noteId,
                                           @RequestHeader("userId") Long userId);

    @GetMapping("/notes/{noteId}/collect/status")
    Result<Boolean> collectStatus(@PathVariable("noteId") Long noteId,
                                  @RequestHeader("userId") Long userId);
}
