package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动标签绑定实体
 */
@Data
@TableName("activity_tags")
public class ActivityTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    /**
     * 标签名称
     */
    private String tagName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
