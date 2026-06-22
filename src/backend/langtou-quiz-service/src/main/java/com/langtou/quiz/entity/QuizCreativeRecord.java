package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.langtou.quiz.enums.CreativeTypeEnum;
import com.langtou.quiz.enums.VariantTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "quiz_creative_record", autoResultMap = true)
public class QuizCreativeRecord {

    @TableId(type = IdType.AUTO)
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

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> interests;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
