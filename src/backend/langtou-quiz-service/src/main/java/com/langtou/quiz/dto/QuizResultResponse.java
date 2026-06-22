package com.langtou.quiz.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuizResultResponse {

    private Long attemptId;

    private Long quizSetId;

    private Integer totalQuestions;

    private Integer correctCount;

    private Integer score;

    private Boolean passed;

    private Integer rank;

    private String title;

    private String shareCardUrl;

    private LocalDateTime createdAt;
}
