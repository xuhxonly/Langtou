package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记-商品关联实体
 */
@Data
@TableName("note_products")
public class NoteProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long productId;

    private Long creatorId;

    private Integer sortOrder;

    /**
     * 状态：0-移除 / 1-正常
     */
    private Integer status;

    private Integer clickCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
