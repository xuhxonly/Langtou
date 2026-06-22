package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品标签实体
 */
@Data
@TableName("product_tags")
public class ProductTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String tagName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
