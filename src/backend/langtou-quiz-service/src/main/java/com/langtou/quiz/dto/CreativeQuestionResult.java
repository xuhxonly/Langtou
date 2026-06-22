package com.langtou.quiz.dto;

import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class CreativeQuestionResult {

    private Long id;

    private Long userId;

    private CreativeTypeEnum creativeType;

    private String subject;

    private String stem;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctAnswer;

    private String explanation;

    private BigDecimal difficulty;

    private VariantTypeEnum variantType;

    private Long originalQuestionId;

    private String context;

    private List<String> interests;

    private Map<String, Object> metadata;
}
