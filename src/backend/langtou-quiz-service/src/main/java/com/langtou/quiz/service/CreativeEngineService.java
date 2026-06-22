package com.langtou.quiz.service;

import com.langtou.quiz.dto.CreativeQuestionResult;
import com.langtou.quiz.dto.SkillLevelResult;
import com.langtou.quiz.enums.VariantTypeEnum;

import java.util.List;

public interface CreativeEngineService {

    CreativeQuestionResult generateSituationalQuestion(Long userId, String subject,
                                                       List<String> interests,
                                                       SkillLevelResult difficulty);

    CreativeQuestionResult generateVariantQuestion(Long userId, Long originalQuestionId,
                                                  VariantTypeEnum variantType);

    CreativeQuestionResult generateStoryQuestion(Long userId, String subject, String storyContext);

    List<CreativeQuestionResult> generateCreativeBatch(Long userId, String subject, int count,
                                                      SkillLevelResult difficulty);

    void saveCreativeRecord(CreativeQuestionResult result);
}
