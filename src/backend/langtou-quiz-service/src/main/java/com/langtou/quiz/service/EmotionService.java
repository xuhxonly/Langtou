package com.langtou.quiz.service;

import com.langtou.quiz.dto.EmotionContext;
import com.langtou.quiz.dto.EmotionDetectionResult;
import com.langtou.quiz.dto.EmotionState;
import com.langtou.quiz.dto.EmotionalResponse;
import com.langtou.quiz.enums.EmotionEnum;

public interface EmotionService {

    EmotionDetectionResult detectEmotion(Long userId, EmotionContext context);

    EmotionalResponse generateResponse(Long userId, EmotionEnum emotion);

    void recordEmotion(Long userId, EmotionEnum emotion, String trigger);

    EmotionState getCurrentState(Long userId);
}
