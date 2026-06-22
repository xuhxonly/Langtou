package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameInventoryVO {

    private Long id;

    private Long itemId;

    private String itemName;

    private String itemType;

    private String rarity;

    private Integer quantity;

    private Boolean equipped;

    private LocalDateTime expiresAt;

    private String iconUrl;
}
