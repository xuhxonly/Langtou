package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_leaderboard")
public class GameLeaderboard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gameId;

    private Long userId;

    private Integer score;

    private Integer rank;

    private Long seasonId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
