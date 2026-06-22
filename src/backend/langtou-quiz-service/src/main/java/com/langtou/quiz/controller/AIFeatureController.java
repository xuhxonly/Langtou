﻿package com.langtou.quiz.controller;

import com.langtou.common.result.Result;
import com.langtou.quiz.dto.*;
import com.langtou.quiz.entity.UserSkillProfile;
import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.EmotionEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import com.langtou.quiz.service.CreativeEngineService;
import com.langtou.quiz.service.EmotionService;
import com.langtou.quiz.service.HintService;
import com.langtou.quiz.service.KnowledgeConnectorService;
import com.langtou.quiz.service.SkillProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz/ai")
@RequiredArgsConstructor
@Validated
public class AIFeatureController {

    private final SkillProfileService skillProfileService;
    private final HintService hintService;
    private final EmotionService emotionService;
    private final CreativeEngineService creativeEngineService;
    private final KnowledgeConnectorService knowledgeConnectorService;

    @GetMapping("/profile")
    public Result<UserSkillProfile> getProfile(
            @RequestParam String subject,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(skillProfileService.getProfile(userId, subject));
    }

    @GetMapping("/profiles")
    public Result<List<UserSkillProfile>> getProfiles(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(skillProfileService.getProfiles(userId));
    }

    @PostMapping("/analyze-profile")
    public Result<SkillAnalysisResult> analyzeProfile(
            @Valid @RequestBody AnalyzeProfileRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(skillProfileService.analyzeProfile(userId, request.getSubject()));
    }

    @GetMapping("/difficulty")
    public Result<SkillLevelResult> predictDifficulty(
            @Valid DifficultyQueryRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        DifficultyContext ctx = new DifficultyContext();
        ctx.setRecentCorrect(request.getRecentCorrect());
        ctx.setRecentStreak(request.getRecentStreak());
        ctx.setTimeSpentMs(request.getTimeSpentMs());
        ctx.setIsTired(request.getIsTired());
        ctx.setConsecutiveQuestions(request.getConsecutiveQuestions());
        ctx.setInGame(request.getInGame());
        ctx.setCurrentScore(request.getCurrentScore());
        ctx.setLivesRemaining(request.getLivesRemaining());
        ctx.setIsRevived(request.getIsRevived());
        return Result.success(skillProfileService.predictNextDifficulty(userId, request.getSubject(), ctx));
    }

    @PostMapping("/hint")
    public Result<HintResponse> requestHint(
            @Valid @RequestBody HintRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        int level = request.getCurrentHintLevel() == null ? 0 : request.getCurrentHintLevel();
        return Result.success(hintService.requestHint(userId, request.getQuizSetId(), request.getQuestionId(), level));
    }

    @PostMapping("/update-after-answer")
    public Result<Void> updateAfterAnswer(
            @RequestParam String subject,
            @RequestParam boolean correct,
            @RequestParam(defaultValue = "0") int responseMs,
            @RequestHeader("X-User-Id") Long userId) {
        skillProfileService.updateAfterAnswer(userId, subject, correct, responseMs);
        return Result.success(null);
    }

    @GetMapping("/emotion/state")
    public Result<EmotionState> getEmotionState(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(emotionService.getCurrentState(userId));
    }

    @PostMapping("/emotion/detect")
    public Result<EmotionDetectionResult> detectEmotion(
            @Valid @RequestBody EmotionContext context,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(emotionService.detectEmotion(userId, context));
    }

    @PostMapping("/emotion/respond")
    public Result<EmotionalResponse> emotionRespond(
            @RequestParam(required = false) String emotion,
            @RequestHeader("X-User-Id") Long userId) {
        EmotionEnum emotionEnum = emotion == null ? null : EmotionEnum.fromValue(emotion);
        if (emotionEnum == null) {
            EmotionState state = emotionService.getCurrentState(userId);
            emotionEnum = state.getCurrentEmotion() == null ? EmotionEnum.CALM : state.getCurrentEmotion();
        }
        return Result.success(emotionService.generateResponse(userId, emotionEnum));
    }

    @PostMapping("/creative/generate")
    public Result<CreativeQuestionResult> generateCreative(
            @Valid @RequestBody CreativeGenerateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        CreativeTypeEnum type = request.getCreativeType() == null
                ? CreativeTypeEnum.SITUATIONAL : request.getCreativeType();
        switch (type) {
            case SITUATIONAL:
            case GAME:
            case PUZZLE:
                SkillLevelResult diff = new SkillLevelResult();
                diff.setRecommendedDifficulty(request.getDifficulty());
                return Result.success(creativeEngineService.generateSituationalQuestion(
                        userId, request.getSubject(), request.getInterests(), diff));
            case VARIANT:
                return Result.success(creativeEngineService.generateVariantQuestion(
                        userId, request.getOriginalQuestionId(), request.getVariantType()));
            case STORY:
                return Result.success(creativeEngineService.generateStoryQuestion(
                        userId, request.getSubject(), request.getStoryContext()));
            default:
                SkillLevelResult diff2 = new SkillLevelResult();
                diff2.setRecommendedDifficulty(request.getDifficulty());
                return Result.success(creativeEngineService.generateSituationalQuestion(
                        userId, request.getSubject(), request.getInterests(), diff2));
        }
    }

    @PostMapping("/creative/batch")
    public Result<List<CreativeQuestionResult>> generateCreativeBatch(
            @Valid @RequestBody CreativeGenerateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        int count = request.getCount() == null ? 5 : request.getCount();
        SkillLevelResult diff = new SkillLevelResult();
        diff.setRecommendedDifficulty(request.getDifficulty());
        return Result.success(creativeEngineService.generateCreativeBatch(
                userId, request.getSubject(), count, diff));
    }

    @GetMapping("/knowledge/connections")
    public Result<KnowledgeConnectionResult> getKnowledgeConnections(
            @RequestParam(required = false) Long questionId,
            @RequestParam(required = false) String topic) {
        if (questionId != null) {
            return Result.success(knowledgeConnectorService.getConnections(questionId));
        }
        return Result.success(knowledgeConnectorService.getConnectionsByTopic(topic));
    }

    @GetMapping("/creative/variants")
    public Result<CreativeQuestionResult> getCreativeVariants(
            @RequestParam Long originalQuestionId,
            @RequestParam(defaultValue = "ROTATE") String variantType,
            @RequestHeader("X-User-Id") Long userId) {
        VariantTypeEnum type = VariantTypeEnum.fromValue(variantType);
        if (type == null) {
            type = VariantTypeEnum.ROTATE;
        }
        return Result.success(creativeEngineService.generateVariantQuestion(userId, originalQuestionId, type));
    }
}
