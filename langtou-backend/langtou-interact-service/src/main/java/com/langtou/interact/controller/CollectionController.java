package com.langtou.interact.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.utils.PageUtils;
import com.langtou.interact.entity.Collection;
import com.langtou.interact.service.CollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping("/notes/{noteId}/collect")
    public Result<Void> collect(@PathVariable Long noteId,
                                 @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        collectionService.collect(userId, noteId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/notes/{noteId}/collect")
    public Result<Void> uncollect(@PathVariable Long noteId,
                                  @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        collectionService.uncollect(userId, noteId);
        return Result.success("取消收藏成功");
    }

    @GetMapping("/users/me/collections")
    public Result<PageUtils.PageResult<Collection>> myCollections(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Page<Collection> page = collectionService.getMyCollections(userId, current, size);
        return Result.success(PageUtils.PageResult.of(page));
    }
}
