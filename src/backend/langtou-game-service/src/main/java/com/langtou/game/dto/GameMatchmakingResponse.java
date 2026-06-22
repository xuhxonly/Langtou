package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameMatchmakingResponse {

    private Long id;

    private Long userId;

    private Long gameId;

    private Integer mmr;

    private String queueType;

    private String status;

    private Integer expectedWaitTime;

    private LocalDateTime createdAt;
}
