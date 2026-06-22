package com.langtou.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizSubmitRequest {

    @NotNull(message = "答题AttemptID不能为空")
    private Long attemptId;

    @NotEmpty(message = "答题列表不能为空")
    @Valid
    private List<AnswerItem> answers;

    private Integer durationSeconds;

    @Data
    public static class AnswerItem {

        @NotNull(message = "题目序号不能为空")
        private Integer sequenceNo;

        @NotNull(message = "选项不能为空")
        private String selected;
    }
}