package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户引导完成记录实体
 */
@Data
@TableName("user_onboarding_progress")
public class UserOnboardingProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long stepId;

    /**
     * 是否完成
     */
    private Boolean completed;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
