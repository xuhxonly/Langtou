package com.langtou.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizGenerateRequest {

    @NotNull(message = "笔记ID不能为空")
    private Long noteId;

    private Integer questionCount;
}
