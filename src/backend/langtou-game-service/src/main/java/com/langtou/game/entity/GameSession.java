package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_session")
public class GameSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gameId;

    private String roomId;

    private Long hostUserId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer maxPlayers;

    private Integer currentPlayers;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
