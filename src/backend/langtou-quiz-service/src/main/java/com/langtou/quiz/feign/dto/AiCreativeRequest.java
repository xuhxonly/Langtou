package com.langtou.quiz.feign.dto;

import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AiCreativeRequest {

    private CreativeTypeEnum creativeType;

    private String subject;

    private List<String> interests;

    private String storyContext;

    private Long originalQuestionId;

    private VariantTypeEnum variantType;

    private BigDecimal difficulty;

    private Integer count;

    private Long userId;

    private Map<String, Object> extra;
}
