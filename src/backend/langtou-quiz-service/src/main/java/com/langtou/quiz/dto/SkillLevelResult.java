package com.langtou.quiz.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkillLevelResult {

    private String subject;

    private BigDecimal recommendedDifficulty;

    private Integer level1To10;

    private String strategy;

    private String reason;

    private BigDecimal confidence;
}
