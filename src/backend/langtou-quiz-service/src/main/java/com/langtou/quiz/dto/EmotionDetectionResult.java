﻿package com.langtou.quiz.dto;

import com.langtou.quiz.enums.EmotionEnum;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmotionDetectionResult {

    private EmotionEnum emotion;

    private BigDecimal confidence;

    private String reason;

    private Long responseMs;

    private Double accuracyRate;

    private Integer streak;
}
