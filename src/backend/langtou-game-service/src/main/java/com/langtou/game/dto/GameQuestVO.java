package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameQuestVO {

    private Long id;

    private Long gameId;

    private String title;

    private String description;

    private String type;

    private Integer targetValue;

    private Integer currentValue;

    private Integer rewardPoints;

    private Long rewardItemId;

    private String status;

    private LocalDateTime updatedAt;
}
