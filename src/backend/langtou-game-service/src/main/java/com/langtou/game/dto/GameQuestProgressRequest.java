package com.langtou.game.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GameQuestProgressRequest {

    @NotNull
    private Long questId;

    @NotNull
    private Integer progressValue;
}
