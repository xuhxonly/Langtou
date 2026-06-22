package com.langtou.quiz.service;

import com.langtou.quiz.dto.DifficultyContext;
import com.langtou.quiz.dto.SkillAnalysisResult;
import com.langtou.quiz.dto.SkillLevelResult;
import com.langtou.quiz.entity.UserSkillProfile;

import java.util.List;

public interface SkillProfileService {

    UserSkillProfile getProfile(Long userId, String subject);

    List<UserSkillProfile> getProfiles(Long userId);

    void updateAfterAnswer(Long userId, String subject, boolean isCorrect, int responseMs);

    SkillAnalysisResult analyzeProfile(Long userId, String subject);

    SkillLevelResult predictNextDifficulty(Long userId, String subject, DifficultyContext context);
}
