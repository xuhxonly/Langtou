package com.langtou.quiz.dto;

import lombok.Data;

@Data
public class EmotionContext {

    private Long avgResponseMs;

    private Long lastResponseMs;

    private Double accuracyRate;

    private Integer recentCorrectCount;

    private Integer recentWrongCount;

    private Integer sessionDurationSeconds;

    private Integer sessionQuestions;

    private Boolean inGame;

    private Integer currentScore;

    private Integer livesRemaining;
}
