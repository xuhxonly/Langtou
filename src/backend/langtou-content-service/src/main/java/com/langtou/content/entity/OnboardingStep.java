package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新手引导步骤实体
 */
@Data
@TableName("onboarding_steps")
public class OnboardingStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 步骤顺序
     */
    private Integer stepOrder;

    /**
     * 步骤标题
     */
    private String title;

    /**
     * 步骤描述
     */
    private String description;

    /**
     * 步骤图片URL
     */
    private String imageUrl;

    /**
     * 动作类型: SELECT_INTERESTS/FOLLOW_CREATORS/PUBLISH_FIRST_NOTE/COMPLETE_PROFILE
     */
    private String actionType;

    /**
     * 目标跳转URL
     */
    private String targetUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
