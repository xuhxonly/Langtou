package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_inventory")
public class GameInventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private String itemType;

    private Integer quantity;

    private Boolean equipped;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
