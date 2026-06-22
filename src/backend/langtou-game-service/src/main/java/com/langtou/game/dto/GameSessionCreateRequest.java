package com.langtou.game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameSessionCreateRequest {

    @NotNull
    private Long gameId;

    private Integer maxPlayers;

    private String roomId;
}
