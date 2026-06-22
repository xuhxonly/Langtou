package com.langtou.quiz.dto;

import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreativeGenerateRequest {

    private CreativeTypeEnum creativeType;

    private String subject;

    private List<String> interests;

    private String storyContext;

    private Long originalQuestionId;

    private VariantTypeEnum variantType;

    private BigDecimal difficulty;

    private Integer count;
}
