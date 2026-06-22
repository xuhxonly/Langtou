package com.langtou.quiz.feign.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AiCreativeResponse {

    private List<CreativeItem> items;

    @Data
    public static class CreativeItem {

        private String creativeType;

        private String subject;

        private String stem;

        private String optionA;

        private String optionB;

        private String optionC;

        private String optionD;

        private String correctAnswer;

        private String explanation;

        private BigDecimal difficulty;

        private String context;

        private Map<String, Object> metadata;
    }
}
