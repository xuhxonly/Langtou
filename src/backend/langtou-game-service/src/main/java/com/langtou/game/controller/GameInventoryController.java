package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.game.dto.GameInventoryVO;
import com.langtou.game.service.GameInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/inventories")
@RequiredArgsConstructor
@Tag(name = "玩家背包", description = "背包查询、道具使用与装备")
@SecurityRequirement(name = "bearer-jwt")
public class GameInventoryController {

    private final GameInventoryService inventoryService;

    @GetMapping
    @Operation(summary = "查询我的背包")
    public Result<List<GameInventoryVO>> list(@RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(inventoryService.listByUserId(userId));
    }

    @PostMapping("/{inventoryId}/use")
    @Operation(summary = "使用道具")
    public Result<GameInventoryVO> use(@PathVariable Long inventoryId,
                                       @RequestParam(defaultValue = "1") Integer quantity,
                                       @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(inventoryService.useItem(userId, inventoryId, quantity));
    }

    @PostMapping("/{inventoryId}/equip")
    @Operation(summary = "装备/卸下道具")
    public Result<GameInventoryVO> equip(@PathVariable Long inventoryId,
                                         @RequestParam Boolean equipped,
                                         @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(inventoryService.equipItem(userId, inventoryId, equipped));
    }
}
