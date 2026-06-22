package com.langtou.quiz.feign.dto;

import lombok.Data;

@Data
public class AiGenerateRequest {

    private Long noteId;

    private Integer questionCount;

    private String difficulty;
}
