package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.langtou.quiz.enums.SkillSubjectEnum;
import com.langtou.quiz.enums.TrendEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "user_skill_profile", autoResultMap = true)
public class UserSkillProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private SkillSubjectEnum subject;

    private BigDecimal skillLevel;

    private BigDecimal confidence;

    private TrendEnum trend;

    private Integer avgResponseMs;

    private BigDecimal accuracyRate;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> weakness;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> learningStyle;

    private Integer totalQuestionsAnswered;

    private Integer totalCorrect;

    private Integer totalWrong;

    private LocalDateTime lastAnsweredAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
