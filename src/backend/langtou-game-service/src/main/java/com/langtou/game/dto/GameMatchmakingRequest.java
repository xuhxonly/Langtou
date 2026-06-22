package com.langtou.game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameMatchmakingRequest {

    @NotNull
    private Long gameId;

    private String queueType;

    private Integer currentMmr;
}
