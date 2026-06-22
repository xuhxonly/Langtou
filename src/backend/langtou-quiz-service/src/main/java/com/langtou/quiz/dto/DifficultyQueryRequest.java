package com.langtou.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DifficultyQueryRequest {

    @NotNull(message = "学科不能为空")
    private String subject;

    private Boolean recentCorrect;

    private Integer recentStreak;

    private Long timeSpentMs;

    private Boolean isTired;

    private Integer consecutiveQuestions;

    private Boolean inGame;

    private Integer currentScore;

    private Integer livesRemaining;

    private Boolean isRevived;
}
