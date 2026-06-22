package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.game.dto.GameQuestProgressRequest;
import com.langtou.game.dto.GameQuestVO;
import com.langtou.game.service.GameQuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/quests")
@RequiredArgsConstructor
@Tag(name = "任务与成就", description = "任务领取、完成、进度追踪")
@SecurityRequirement(name = "bearer-jwt")
public class GameQuestController {

    private final GameQuestService questService;

    @GetMapping
    @Operation(summary = "查询游戏任务列表")
    public Result<List<GameQuestVO>> list(@RequestParam Long gameId) {
        return Result.success(questService.listByGameId(gameId));
    }

    @PostMapping("/{questId}/claim")
    @Operation(summary = "领取/完成任务奖励")
    public Result<GameQuestVO> claim(@PathVariable Long questId,
                                     @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(questService.claimQuest(questId, userId));
    }

    @PostMapping("/progress")
    @Operation(summary = "上报任务进度")
    public Result<GameQuestVO> trackProgress(@Valid @RequestBody GameQuestProgressRequest request,
                                             @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(questService.trackProgress(userId, request));
    }

    @PostMapping("/{questId}/complete")
    @Operation(summary = "完成任务")
    public Result<GameQuestVO> complete(@PathVariable Long questId,
                                       @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(questService.completeQuest(questId, userId));
    }
}
