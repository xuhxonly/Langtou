package com.langtou.interact.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collection")
public class Collection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long noteId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
