package com.langtou.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HintRequest {

    @NotNull(message = "测验集ID不能为空")
    private Long quizSetId;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    private Integer currentHintLevel;
}
