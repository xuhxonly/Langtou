package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_session_players")
public class GameSessionPlayer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private LocalDateTime joinedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
