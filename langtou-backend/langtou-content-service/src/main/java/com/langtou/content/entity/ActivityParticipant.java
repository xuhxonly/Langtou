package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 活动参与记录实体
 */
@Data
@TableName("activity_participants")
public class ActivityParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    /**
     * 参与笔记数
     */
    private Integer noteCount;

    /**
     * 参与时间
     */
    private LocalDateTime joinedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
