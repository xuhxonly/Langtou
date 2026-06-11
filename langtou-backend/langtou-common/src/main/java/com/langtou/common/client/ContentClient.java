package com.langtou.common.client;

import com.langtou.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.Map;

@FeignClient(name = "langtou-content-service", path = "/api/v1")
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
}
