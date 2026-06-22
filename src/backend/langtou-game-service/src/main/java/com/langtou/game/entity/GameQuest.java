package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_quest")
public class GameQuest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gameId;

    private String title;

    private String description;

    private String type;

    private Integer targetValue;

    private Integer rewardPoints;

    private Long rewardItemId;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
