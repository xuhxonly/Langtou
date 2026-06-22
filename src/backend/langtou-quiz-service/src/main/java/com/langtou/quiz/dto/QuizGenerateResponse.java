package com.langtou.quiz.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizGenerateResponse {

    private Long quizSetId;

    private Long noteId;

    private Integer questionCount;

    private String status;

    private List<QuestionItem> questions;

    private LocalDateTime expiresAt;

    @Data
    public static class QuestionItem {
        private Integer sequenceNo;
        private String stem;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String explanation;
    }
}
