package com.langtou.quiz.feign.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiGenerateResponse {

    private Long noteId;

    private List<QuestionItem> questions;

    private String promptHash;

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

        private Integer score;
    }
}
