package com.langtou.quiz.service.impl;

import com.langtou.common.result.Result;
import com.langtou.quiz.dto.CreativeGenerateRequest;
import com.langtou.quiz.dto.CreativeQuestionResult;
import com.langtou.quiz.dto.SkillLevelResult;
import com.langtou.quiz.entity.QuizCreativeRecord;
import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import com.langtou.quiz.feign.AiCreativeClient;
import com.langtou.quiz.feign.dto.AiCreativeRequest;
import com.langtou.quiz.feign.dto.AiCreativeResponse;
import com.langtou.quiz.mapper.QuizCreativeRecordMapper;
import com.langtou.quiz.service.CreativeEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreativeEngineServiceImpl implements CreativeEngineService {

    private final AiCreativeClient aiCreativeClient;
    private final QuizCreativeRecordMapper quizCreativeRecordMapper;

    @Override
    public CreativeQuestionResult generateSituationalQuestion(Long userId, String subject,
                                                              List<String> interests,
                                                              SkillLevelResult difficulty) {
        CreativeGenerateRequest request = new CreativeGenerateRequest();
        request.setCreativeType(CreativeTypeEnum.SITUATIONAL);
        request.setSubject(subject);
        request.setInterests(interests);
        request.setDifficulty(difficulty != null ? difficulty.getRecommendedDifficulty() : BigDecimal.valueOf(5));
        AiCreativeRequest feignRequest = toFeignRequest(request, userId);
        return callAiAndPick(feignRequest, CreativeTypeEnum.SITUATIONAL, userId, subject,
                difficulty == null ? null : difficulty.getRecommendedDifficulty());
    }

    @Override
    public CreativeQuestionResult generateVariantQuestion(Long userId, Long originalQuestionId,
                                                         VariantTypeEnum variantType) {
        CreativeGenerateRequest request = new CreativeGenerateRequest();
        request.setCreativeType(CreativeTypeEnum.VARIANT);
        request.setVariantType(variantType);
        request.setOriginalQuestionId(originalQuestionId);
        AiCreativeRequest feignRequest = toFeignRequest(request, userId);
        CreativeQuestionResult result = callAiAndPick(feignRequest, CreativeTypeEnum.VARIANT,
                userId, null, null);
        if (result != null) {
            result.setVariantType(variantType);
            result.setOriginalQuestionId(originalQuestionId);
        }
        return result;
    }

    @Override
    public CreativeQuestionResult generateStoryQuestion(Long userId, String subject, String storyContext) {
        CreativeGenerateRequest request = new CreativeGenerateRequest();
        request.setCreativeType(CreativeTypeEnum.STORY);
        request.setSubject(subject);
        request.setStoryContext(storyContext);
        AiCreativeRequest feignRequest = toFeignRequest(request, userId);
        return callAiAndPick(feignRequest, CreativeTypeEnum.STORY, userId, subject, null);
    }

    @Override
    public List<CreativeQuestionResult> generateCreativeBatch(Long userId, String subject, int count,
                                                              SkillLevelResult difficulty) {
        CreativeGenerateRequest request = new CreativeGenerateRequest();
        request.setCreativeType(CreativeTypeEnum.SITUATIONAL);
        request.setSubject(subject);
        request.setCount(count);
        request.setDifficulty(difficulty != null ? difficulty.getRecommendedDifficulty() : BigDecimal.valueOf(5));
        AiCreativeRequest feignRequest = toFeignRequest(request, userId);
        feignRequest.setCount(count);
        List<CreativeQuestionResult> results = callAiBatch(feignRequest, userId, subject,
                difficulty == null ? null : difficulty.getRecommendedDifficulty());
        for (CreativeQuestionResult r : results) {
            saveCreativeRecord(r);
        }
        return results;
    }

    @Override
    @Transactional
    public void saveCreativeRecord(CreativeQuestionResult result) {
        if (result == null) {
            return;
        }
        QuizCreativeRecord record = new QuizCreativeRecord();
        record.setUserId(result.getUserId() != null ? result.getUserId() : 0L);
        record.setCreativeType(result.getCreativeType());
        record.setSubject(result.getSubject());
        record.setStem(result.getStem());
        record.setOptionA(result.getOptionA());
        record.setOptionB(result.getOptionB());
        record.setOptionC(result.getOptionC());
        record.setOptionD(result.getOptionD());
        record.setCorrectAnswer(result.getCorrectAnswer());
        record.setExplanation(result.getExplanation());
        record.setDifficulty(result.getDifficulty());
        record.setVariantType(result.getVariantType());
        record.setOriginalQuestionId(result.getOriginalQuestionId());
        record.setContext(result.getContext());
        record.setInterests(result.getInterests());
        record.setMetadata(result.getMetadata());
        quizCreativeRecordMapper.insert(record);
        result.setId(record.getId());
        log.info("save creative record: id={}, type={}, subject={}", record.getId(),
                record.getCreativeType(), record.getSubject());
    }

    private AiCreativeRequest toFeignRequest(CreativeGenerateRequest request, Long userId) {
        AiCreativeRequest r = new AiCreativeRequest();
        r.setCreativeType(request.getCreativeType());
        r.setSubject(request.getSubject());
        r.setInterests(request.getInterests());
        r.setStoryContext(request.getStoryContext());
        r.setOriginalQuestionId(request.getOriginalQuestionId());
        r.setVariantType(request.getVariantType());
        r.setDifficulty(request.getDifficulty());
        r.setCount(request.getCount());
        r.setUserId(userId);
        return r;
    }

    private CreativeQuestionResult callAiAndPick(AiCreativeRequest feignRequest,
                                                 CreativeTypeEnum fallbackType,
                                                 Long userId, String subject,
                                                 BigDecimal difficulty) {
        List<CreativeQuestionResult> list = callAiBatch(feignRequest, userId, subject, difficulty);
        if (list.isEmpty()) {
            return buildFallback(fallbackType, userId, subject, difficulty);
        }
        return list.get(0);
    }

    private List<CreativeQuestionResult> callAiBatch(AiCreativeRequest feignRequest,
                                                     Long userId, String subject,
                                                     BigDecimal difficulty) {
        List<CreativeQuestionResult> results = new ArrayList<>();
        try {
            Result<AiCreativeResponse> resp = aiCreativeClient.generateCreative(feignRequest);
            if (resp == null || resp.getCode() == null || resp.getCode() != 200
                    || resp.getData() == null || resp.getData().getItems() == null) {
                log.warn("AI creative generation returned empty, fallback used");
                return results;
            }
            for (AiCreativeResponse.CreativeItem item : resp.getData().getItems()) {
                CreativeQuestionResult r = new CreativeQuestionResult();
                r.setCreativeType(CreativeTypeEnum.fromValue(item.getCreativeType()));
                r.setSubject(item.getSubject() != null ? item.getSubject() : subject);
                r.setStem(item.getStem());
                r.setOptionA(item.getOptionA());
                r.setOptionB(item.getOptionB());
                r.setOptionC(item.getOptionC());
                r.setOptionD(item.getOptionD());
                r.setCorrectAnswer(item.getCorrectAnswer());
                r.setExplanation(item.getExplanation());
                r.setDifficulty(item.getDifficulty() != null ? item.getDifficulty() : difficulty);
                r.setContext(item.getContext());
                r.setMetadata(item.getMetadata());
                r.setInterests(feignRequest.getInterests());
                results.add(r);
            }
        } catch (Exception e) {
            log.error("call AI creative service exception, fallback to empty list", e);
        }
        return results;
    }

    private CreativeQuestionResult buildFallback(CreativeTypeEnum type, Long userId,
                                                String subject, BigDecimal difficulty) {
        CreativeQuestionResult r = new CreativeQuestionResult();
        r.setCreativeType(type);
        r.setSubject(subject);
        r.setDifficulty(difficulty == null ? BigDecimal.valueOf(5) : difficulty);
        r.setStem("Please answer based on what you have learned (AI creative service is temporarily unavailable, fallback to basic question)");
        return r;
    }
}
