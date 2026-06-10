package com.langtou.content.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/publish")
    public Result<ContentDTO> publish(@Valid @RequestBody ContentDTO contentDTO,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        ContentDTO result = contentService.publish(contentDTO, userId);
        return Result.success("发布成功", result);
    }

    @GetMapping("/{id}")
    public Result<ContentDTO> getContentById(@PathVariable Long id) {
        ContentDTO result = contentService.getContentById(id);
        return Result.success(result);
    }

    @GetMapping("/user/{userId}")
    public Result<List<ContentDTO>> getUserContents(@PathVariable Long userId) {
        List<ContentDTO> result = contentService.getUserContents(userId);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteContent(@PathVariable Long id,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        contentService.deleteContent(id, userId);
        return Result.success("删除成功");
    }

    @GetMapping("/feed")
    public Result<List<ContentDTO>> getFeed(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        List<ContentDTO> result = contentService.getFeed(page, size);
        return Result.success(result);
    }
}
