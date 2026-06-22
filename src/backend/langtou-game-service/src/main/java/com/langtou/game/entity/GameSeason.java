package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_season")
public class GameSeason {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String status;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
