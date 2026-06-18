package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品实体
 */
@Data
@TableName(value = "products", autoResultMap = true)
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String imageUrl;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String category;

    private String externalUrl;

    private BigDecimal commissionRate;

    /**
     * 状态：AVAILABLE-上架 / UNAVAILABLE-下架
     */
    private String status;

    private Integer salesCount;

    private Integer clickCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
