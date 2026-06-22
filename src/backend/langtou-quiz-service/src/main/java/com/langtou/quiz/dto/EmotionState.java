﻿package com.langtou.quiz.dto;

import com.langtou.quiz.enums.EmotionEnum;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmotionState {

    private EmotionEnum currentEmotion;

    private EmotionEnum previousEmotion;

    private BigDecimal confidence;

    private Integer consecutiveCorrect;

    private Integer consecutiveWrong;

    private Integer sessionQuestions;

    private Double accuracyRate;

    private Long avgResponseMs;

    private Integer sessionDurationSeconds;

    private String lastTrigger;

    private Long detectedAt;
}
