package com.langtou.interact.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.service.InteractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/interact")
@RequiredArgsConstructor
public class InteractController {

    private final InteractService interactService;

    @PostMapping("/like/{contentId}")
    public Result<Void> like(@PathVariable Long contentId,
                             @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.like(userId, contentId);
        return Result.success("点赞成功");
    }

    @PostMapping("/unlike/{contentId}")
    public Result<Void> unlike(@PathVariable Long contentId,
                               @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.unlike(userId, contentId);
        return Result.success("取消点赞成功");
    }

    @GetMapping("/like/status/{contentId}")
    public Result<Map<String, Object>> likeStatus(@PathVariable Long contentId,
                                                   @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        boolean hasLiked = interactService.hasLiked(userId, contentId);
        Long count = interactService.getLikeCount(contentId);
        Map<String, Object> data = Map.of("hasLiked", hasLiked, "count", count);
        return Result.success(data);
    }

    @PostMapping("/comment/{contentId}")
    public Result<Comment> comment(@PathVariable Long contentId,
                                   @RequestBody Map<String, String> params,
                                   @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String content = params.get("content");
        Long parentId = params.containsKey("parentId") && params.get("parentId") != null
                ? Long.valueOf(params.get("parentId")) : null;
        Comment comment = interactService.comment(userId, contentId, content, parentId);
        return Result.success("评论成功", comment);
    }

    @GetMapping("/comment/{contentId}")
    public Result<List<Comment>> getComments(@PathVariable Long contentId) {
        List<Comment> comments = interactService.getComments(contentId);
        return Result.success(comments);
    }

    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.deleteComment(userId, commentId);
        return Result.success("删除成功");
    }
}
