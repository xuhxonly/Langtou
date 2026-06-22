package com.langtou.quiz.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class DifficultyContext {

    private Boolean recentCorrect;

    private Integer recentStreak;

    private Long timeSpentMs;

    private LocalTime currentTime;

    private Boolean isTired;

    private Integer consecutiveQuestions;

    private Boolean inGame;

    private Integer currentScore;

    private Integer livesRemaining;

    private Boolean isRevived;
}
