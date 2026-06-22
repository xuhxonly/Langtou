package com.langtou.interact.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("like_record")
public class LikeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    /**
     * 点赞目标类型: note笔记 comment评论
     */
    private String targetType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
