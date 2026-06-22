package com.langtou.game.service;

import com.langtou.game.dto.GameInventoryVO;

import java.util.List;

public interface GameInventoryService {

    List<GameInventoryVO> listByUserId(Long userId);

    GameInventoryVO useItem(Long userId, Long inventoryId, Integer quantity);

    GameInventoryVO equipItem(Long userId, Long inventoryId, Boolean equipped);
}
