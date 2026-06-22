package com.langtou.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizStartRequest {

    @NotNull(message = "关卡ID不能为空")
    private Long quizSetId;

    private Long gameSessionId;
}
