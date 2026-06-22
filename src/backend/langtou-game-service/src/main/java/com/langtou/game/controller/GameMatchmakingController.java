package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.game.dto.GameMatchmakingRequest;
import com.langtou.game.dto.GameMatchmakingResponse;
import com.langtou.game.service.GameMatchmakingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game/matchmaking")
@RequiredArgsConstructor
@Tag(name = "匹配系统", description = "提交/取消匹配，MMR 计算")
@SecurityRequirement(name = "bearer-jwt")
public class GameMatchmakingController {

    private final GameMatchmakingService matchmakingService;

    @PostMapping("/submit")
    @Operation(summary = "提交匹配")
    public Result<GameMatchmakingResponse> submit(@Valid @RequestBody GameMatchmakingRequest request,
                                                  @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(matchmakingService.submitMatchmaking(request, userId));
    }

    @PostMapping("/{matchmakingId}/cancel")
    @Operation(summary = "取消匹配")
    public Result<Void> cancel(@PathVariable Long matchmakingId,
                               @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        matchmakingService.cancelMatchmaking(matchmakingId, userId);
        return Result.success();
    }

    @GetMapping("/mmr")
    @Operation(summary = "查询当前 MMR")
    public Result<Integer> getMmr(@RequestParam Long gameId,
                                  @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(matchmakingService.calculateMmr(userId, gameId));
    }
}
