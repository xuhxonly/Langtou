package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户等级/积分实体
 */
@Data
@TableName("user_level")
public class UserLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer level;

    private Integer points;

    private Integer experience;

    private Integer totalPoints;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
