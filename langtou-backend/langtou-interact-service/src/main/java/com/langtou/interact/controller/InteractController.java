package com.langtou.interact.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.utils.PageUtils;
import com.langtou.interact.dto.CommentCreateDTO;
import com.langtou.interact.dto.CommentVO;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.entity.ShareRecord;
import com.langtou.interact.service.InteractService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InteractController {

    private final InteractService interactService;

    // ========== 点赞 ==========

    @PostMapping("/notes/{noteId}/like")
    public Result<Void> like(@PathVariable Long noteId,
                             @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.like(userId, noteId);
        return Result.success("点赞成功");
    }

    @DeleteMapping("/notes/{noteId}/like")
    public Result<Void> unlike(@PathVariable Long noteId,
                               @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.unlike(userId, noteId);
        return Result.success("取消点赞成功");
    }

    // ========== 评论 ==========

    @GetMapping("/notes/{noteId}/comments")
    public Result<PageUtils.PageResult<CommentVO>> getComments(
            @PathVariable Long noteId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        Page<CommentVO> page = interactService.getCommentsWithTree(noteId, userId, current, size);
        return Result.success(PageUtils.PageResult.of(page));
    }

    @PostMapping("/notes/{noteId}/comments")
    public Result<Comment> comment(@PathVariable Long noteId,
                                   @RequestBody Map<String, Object> params,
                                   @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String content = params.get("content").toString();
        Long parentId = params.containsKey("parentId") && params.get("parentId") != null
                ? Long.valueOf(params.get("parentId").toString()) : null;
        Comment comment = interactService.comment(userId, noteId, content, parentId);
        return Result.success("评论成功", comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.deleteComment(userId, commentId);
        return Result.success("删除成功");
    }

    @PostMapping("/comments/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId,
                                   @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        interactService.likeComment(userId, commentId);
        return Result.success("点赞评论成功");
    }

    // ========== 转发 ==========

    @PostMapping("/notes/{noteId}/share")
    public Result<ShareRecord> share(@PathVariable Long noteId,
                                    @RequestBody Map<String, String> params,
                                    @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String shareType = params.get("shareType");
        ShareRecord record = interactService.share(userId, noteId, shareType);
        return Result.success("转发成功", record);
    }

    /**
     * 生成笔记分享链接
     */
    @GetMapping("/notes/{noteId}/share-link")
    public Result<Map<String, String>> getShareLink(@PathVariable Long noteId) {
        String shareLink = interactService.generateShareLink(noteId);
        Map<String, String> data = new HashMap<>();
        data.put("shareLink", shareLink);
        return Result.success(data);
    }
}
