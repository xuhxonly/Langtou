package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就实体
 */
@Data
@TableName("user_achievement")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String achievementType;

    private String achievementName;

    private String description;

    private String iconUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime obtainedAt;
}
