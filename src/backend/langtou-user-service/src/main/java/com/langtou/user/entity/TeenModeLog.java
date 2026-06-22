package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 青少年模式使用时长日志实体
 */
@Data
@TableName("teen_mode_logs")
public class TeenModeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 使用日期
     */
    private LocalDate usageDate;

    /**
     * 当日总使用时长（秒）
     */
    private Integer totalSeconds;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
