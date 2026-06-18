package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分记录实体
 */
@Data
@TableName("points_record")
public class PointsRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String actionType;

    private Integer points;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
