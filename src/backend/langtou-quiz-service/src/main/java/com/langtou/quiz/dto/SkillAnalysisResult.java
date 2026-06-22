package com.langtou.quiz.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkillAnalysisResult {

    private Long userId;

    private String subject;

    private BigDecimal overallSkillLevel;

    private BigDecimal accuracyRate;

    private BigDecimal confidence;

    private String trend;

    private List<String> weakPoints;

    private List<String> strongPoints;

    private String learningStyle;

    private Integer totalAnswered;

    private Integer totalCorrect;

    private Integer totalWrong;

    private String suggestion;
}
