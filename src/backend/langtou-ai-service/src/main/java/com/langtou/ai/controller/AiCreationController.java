package com.langtou.ai.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.ai.dto.AiCoverRecommendRequest;
import com.langtou.ai.dto.AiCoverRecommendResponse;
import com.langtou.ai.dto.AiDraftRequest;
import com.langtou.ai.dto.AiDraftResponse;
import com.langtou.ai.dto.AiTagRecommendRequest;
import com.langtou.ai.dto.AiTagRecommendResponse;
import com.langtou.ai.dto.AiTitleRequest;
import com.langtou.ai.dto.AiTitleResponse;
import com.langtou.ai.service.AiCreationService;
import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI创作服务", description = "AI写作助手、标题/封面/标签推荐")
    public class AiCreationController {

    private final AiCreationService aiCreationService;

    @PostMapping("/generate-title")
    @RequireRole
    public Result<AiTitleResponse> generateTitle(@Valid @RequestBody AiTitleRequest request,
                                                  @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI generate title: userId={}, imageCount={}", userId, request.getImageUrls().size());
        aiCreationService.checkRateLimit(userId);
        AiTitleResponse response = aiCreationService.generateTitles(request);
        return Result.success(response);
    }

    @PostMapping("/recommend-tags")
    @RequireRole
    public Result<AiTagRecommendResponse> recommendTags(@Valid @RequestBody AiTagRecommendRequest request,
                                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI recommend tags: userId={}, title={}", userId, request.getTitle());
        aiCreationService.checkRateLimit(userId);
        AiTagRecommendResponse response = aiCreationService.recommendTags(request);
        return Result.success(response);
    }

    @PostMapping("/generate-draft")
    @RequireRole
    public Result<AiDraftResponse> generateDraft(@Valid @RequestBody AiDraftRequest request,
                                                  @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI generate draft: userId={}, title={}, style={}", userId, request.getTitle(), request.getStyle());
        aiCreationService.checkRateLimit(userId);
        AiDraftResponse response = aiCreationService.generateDraft(request);
        return Result.success(response);
    }

    @PostMapping("/recommend-cover")
    @RequireRole
    public Result<AiCoverRecommendResponse> recommendCover(@Valid @RequestBody AiCoverRecommendRequest request,
                                                          @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        log.info("AI recommend cover: userId={}, imageCount={}", userId, request.getImageUrls().size());
        aiCreationService.checkRateLimit(userId);
        AiCoverRecommendResponse response = aiCreationService.recommendCover(request);
        return Result.success(response);
    }
}