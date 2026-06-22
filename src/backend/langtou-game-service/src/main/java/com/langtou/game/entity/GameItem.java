package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("game_item")
public class GameItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String rarity;

    private String description;

    private String iconUrl;

    private Boolean stackable;

    private Integer maxStack;

    private BigDecimal price;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
