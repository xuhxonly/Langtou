package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个性化推荐控制器
 * 提供个性化推荐流、热门推荐、行为反馈等接口
 */
@Tag(name = "个性化推荐", description = "用户画像驱动的个性化推荐系统")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 个性化推荐流
     * 基于用户画像和行为特征返回个性化笔记列表
     *
     * @param userId 用户ID（从请求头获取）
     * @param page   页码，默认1
     * @param size   每页数量，默认20
     * @return 推荐笔记列表
     */
    @Operation(summary = "个性化推荐流", description = "基于用户画像返回个性化笔记推荐列表")
    @GetMapping("/feed")
    public Result<List<NoteFeedVO>> recommendFeed(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NoteFeedVO> result = recommendationService.recommendFeed(userId, page, size);
        return Result.success(result);
    }

    /**
     * 热门推荐
     * 返回全站热门笔记，适用于未登录用户或探索场景
     *
     * @param page 页码，默认1
     * @param size 每页数量，默认20
     * @return 热门笔记列表
     */
    @Operation(summary = "热门推荐", description = "返回全站热门笔记，适用于未登录用户")
    @GetMapping("/hot")
    public Result<List<NoteFeedVO>> hotRecommend(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<NoteFeedVO> result = recommendationService.recommendForNewUser(page, size);
        return Result.success(result);
    }

    /**
     * 行为反馈接口
     * 接收用户行为（浏览/点赞/评论/收藏/分享），用于实时更新推荐结果
     *
     * @param userId 用户ID（从请求头获取）
     * @param body   请求体，包含 noteId 和 actionType
     * @return 操作结果
     */
    @Operation(summary = "行为反馈", description = "上报用户行为，实时影响推荐结果（5分钟内生效）")
    @PostMapping("/feedback")
    public Result<Void> recordFeedback(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestBody Map<String, Object> body) {
        Long noteId = body.get("noteId") != null ? Long.valueOf(body.get("noteId").toString()) : null;
        String actionType = body.get("actionType") != null ? body.get("actionType").toString() : null;

        if (noteId == null || actionType == null) {
            return Result.error("noteId 和 actionType 不能为空");
        }

        recommendationService.recordUserAction(userId, noteId, actionType);
        return Result.success("反馈已记录");
    }
}
