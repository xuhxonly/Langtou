package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameLeaderboardVO {

    private Long id;

    private Long gameId;

    private Long userId;

    private String userNickname;

    private Integer score;

    private Integer rank;

    private Long seasonId;

    private LocalDateTime updatedAt;
}
