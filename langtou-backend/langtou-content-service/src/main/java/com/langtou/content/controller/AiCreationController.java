package com.langtou.content.controller;

import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.content.dto.*;
import com.langtou.content.service.AiCreationService;
import com.langtou.content.service.impl.AiCreationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI创作工具Controller
 *
 * 提供AI辅助创作能力的REST API，包括标题生成、标签推荐、文案草稿和封面推荐。
 * 所有接口需要登录验证。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiCreationController {

    private final AiCreationService aiCreationService;
    private final AiCreationServiceImpl aiCreationServiceImpl;

    /**
     * AI生成标题
     *
     * 根据上传的图片自动生成3-5个候选标题，每个标题附带推荐理由和评分。
     *
     * @param request  标题生成请求
     * @param userId   当前登录用户ID
     * @return 标题建议列表
     */
    @PostMapping("/generate-title")
    @RequireRole
    public Result<AiTitleResponse> generateTitle(@Valid @RequestBody AiTitleRequest request,
                                                  @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI生成标题请求: userId={}, imageCount={}", userId, request.getImageUrls().size());
        aiCreationServiceImpl.checkRateLimit(userId);
        AiTitleResponse response = aiCreationService.generateTitles(request);
        return Result.success(response);
    }

    /**
     * AI推荐标签
     *
     * 根据图片内容和标题推荐5-10个话题标签，包含标签热度和分类信息。
     *
     * @param request  标签推荐请求
     * @param userId   当前登录用户ID
     * @return 标签建议列表
     */
    @PostMapping("/recommend-tags")
    @RequireRole
    public Result<AiTagRecommendResponse> recommendTags(@Valid @RequestBody AiTagRecommendRequest request,
                                                          @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI推荐标签请求: userId={}, title={}", userId, request.getTitle());
        aiCreationServiceImpl.checkRateLimit(userId);
        AiTagRecommendResponse response = aiCreationService.recommendTags(request);
        return Result.success(response);
    }

    /**
     * AI生成文案草稿
     *
     * 根据标题和文案风格生成完整的笔记正文草稿，支持种草/测评/教程/Vlog四种风格。
     *
     * @param request  文案草稿请求
     * @param userId   当前登录用户ID
     * @return 文案草稿内容和字数
     */
    @PostMapping("/generate-draft")
    @RequireRole
    public Result<AiDraftResponse> generateDraft(@Valid @RequestBody AiDraftRequest request,
                                                    @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI生成文案草稿请求: userId={}, title={}, style={}", userId, request.getTitle(), request.getStyle());
        aiCreationServiceImpl.checkRateLimit(userId);
        AiDraftResponse response = aiCreationService.generateDraft(request);
        return Result.success(response);
    }

    /**
     * AI推荐封面
     *
     * 从多张图片中推荐最佳封面，返回每张图片的构图、色彩、清晰度评分和综合评分。
     *
     * @param request  封面推荐请求
     * @param userId   当前登录用户ID
     * @return 封面建议列表（按评分降序）
     */
    @PostMapping("/recommend-cover")
    @RequireRole
    public Result<AiCoverRecommendResponse> recommendCover(@Valid @RequestBody AiCoverRecommendRequest request,
                                                              @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI推荐封面请求: userId={}, imageCount={}", userId, request.getImageUrls().size());
        aiCreationServiceImpl.checkRateLimit(userId);
        AiCoverRecommendResponse response = aiCreationService.recommendCover(request);
        return Result.success(response);
    }
}
