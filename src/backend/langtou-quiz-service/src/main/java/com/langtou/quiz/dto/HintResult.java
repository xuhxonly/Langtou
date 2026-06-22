package com.langtou.quiz.dto;

import lombok.Data;

@Data
public class HintResult {

    private Integer level;

    private String title;

    private String content;

    private String hintType;
}
