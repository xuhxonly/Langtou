package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameSessionResponse {

    private Long id;

    private Long gameId;

    private String roomId;

    private Long hostUserId;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer maxPlayers;

    private Integer currentPlayers;
}
