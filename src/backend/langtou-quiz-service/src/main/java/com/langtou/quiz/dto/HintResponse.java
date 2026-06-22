package com.langtou.quiz.dto;

import lombok.Data;

@Data
public class HintResponse {

    private Long questionId;

    private Integer currentHintLevel;

    private Integer nextHintLevel;

    private String hintContent;

    private Boolean hasMoreHints;

    private String strategy;
}
