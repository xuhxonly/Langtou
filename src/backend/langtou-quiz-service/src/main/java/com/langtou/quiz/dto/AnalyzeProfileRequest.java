package com.langtou.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalyzeProfileRequest {

    @NotNull(message = "学科不能为空")
    private String subject;
}
