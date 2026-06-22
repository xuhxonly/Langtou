package com.langtou.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GameInventoryVO;
import com.langtou.game.entity.GameInventory;
import com.langtou.game.entity.GameItem;
import com.langtou.game.mapper.GameInventoryMapper;
import com.langtou.game.mapper.GameItemMapper;
import com.langtou.game.service.GameInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameInventoryServiceImpl implements GameInventoryService {

    private final GameInventoryMapper gameInventoryMapper;
    private final GameItemMapper gameItemMapper;

    @Override
    public List<GameInventoryVO> listByUserId(Long userId) {
        List<GameInventory> inventories = gameInventoryMapper.selectList(
                new LambdaQueryWrapper<GameInventory>()
                        .eq(GameInventory::getUserId, userId));
        if (inventories == null || inventories.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> itemIds = inventories.stream()
                .map(GameInventory::getItemId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, GameItem> itemMap = gameItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(GameItem::getId, i -> i));
        return inventories.stream().map(inv -> {
            GameInventoryVO vo = new GameInventoryVO();
            vo.setId(inv.getId());
            vo.setItemId(inv.getItemId());
            vo.setItemType(inv.getItemType());
            vo.setQuantity(inv.getQuantity());
            vo.setEquipped(inv.getEquipped());
            vo.setExpiresAt(inv.getExpiresAt());
            GameItem item = itemMap.get(inv.getItemId());
            if (item != null) {
                vo.setItemName(item.getName());
                vo.setRarity(item.getRarity());
                vo.setIconUrl(item.getIconUrl());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public GameInventoryVO useItem(Long userId, Long inventoryId, Integer quantity) {
        GameInventory inventory = gameInventoryMapper.selectById(inventoryId);
        if (inventory == null || !inventory.getUserId().equals(userId)) {
            throw new BusinessException("道具不存在");
        }
        int qty = quantity == null || quantity <= 0 ? 1 : quantity;
        if (inventory.getQuantity() < qty) {
            throw new BusinessException("道具数量不足");
        }
        inventory.setQuantity(inventory.getQuantity() - qty);
        if (inventory.getQuantity() <= 0) {
            gameInventoryMapper.deleteById(inventoryId);
        } else {
            gameInventoryMapper.updateById(inventory);
        }
        return toVO(inventory);
    }

    @Override
    public GameInventoryVO equipItem(Long userId, Long inventoryId, Boolean equipped) {
        GameInventory inventory = gameInventoryMapper.selectById(inventoryId);
        if (inventory == null || !inventory.getUserId().equals(userId)) {
            throw new BusinessException("道具不存在");
        }
        if (!StringUtils.hasText(inventory.getItemType())) {
            throw new BusinessException("该道具不支持装备");
        }
        inventory.setEquipped(equipped);
        gameInventoryMapper.updateById(inventory);
        return toVO(inventory);
    }

    private GameInventoryVO toVO(GameInventory inventory) {
        GameInventoryVO vo = new GameInventoryVO();
        vo.setId(inventory.getId());
        vo.setItemId(inventory.getItemId());
        vo.setItemType(inventory.getItemType());
        vo.setQuantity(inventory.getQuantity());
        vo.setEquipped(inventory.getEquipped());
        vo.setExpiresAt(inventory.getExpiresAt());
        GameItem item = gameItemMapper.selectById(inventory.getItemId());
        if (item != null) {
            vo.setItemName(item.getName());
            vo.setRarity(item.getRarity());
            vo.setIconUrl(item.getIconUrl());
        }
        return vo;
    }
}
