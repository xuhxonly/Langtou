package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_matchmaking")
public class GameMatchmaking {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long gameId;

    private Integer mmr;

    private String queueType;

    private String status;

    private Integer expectedWaitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
