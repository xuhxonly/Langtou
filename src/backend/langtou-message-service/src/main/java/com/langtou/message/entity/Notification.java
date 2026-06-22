package com.langtou.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 通知类型: LIKE, COMMENT, FOLLOW, COLLECT, SHARE, SYSTEM
     */
    private String type;

    /**
     * 来源ID
     */
    private Long sourceId;

    /**
     * 来源类型
     */
    private String sourceType;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 是否已读: 0-未读, 1-已读
     */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
