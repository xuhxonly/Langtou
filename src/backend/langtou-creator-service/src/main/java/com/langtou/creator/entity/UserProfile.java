package com.langtou.creator.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Double> interestTags;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> behaviorFeatures;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> demographicFeatures;

    private LocalDateTime lastActiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
