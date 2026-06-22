package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.game.dto.GameLeaderboardVO;
import com.langtou.game.service.GameLeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game/leaderboards")
@RequiredArgsConstructor
@Tag(name = "排行榜", description = "排行榜查询与排名更新")
@SecurityRequirement(name = "bearer-jwt")
public class GameLeaderboardController {

    private final GameLeaderboardService leaderboardService;

    @GetMapping
    @Operation(summary = "查询排行榜")
    public Result<List<GameLeaderboardVO>> list(@RequestParam Long gameId,
                                                @RequestParam(required = false) Long seasonId,
                                                @RequestParam(defaultValue = "20") int limit) {
        return Result.success(leaderboardService.getLeaderboard(gameId, seasonId, limit));
    }

    @GetMapping("/mine")
    @Operation(summary = "查询我的排名")
    public Result<GameLeaderboardVO> mine(@RequestParam Long gameId,
                                          @RequestParam(required = false) Long seasonId,
                                          @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(leaderboardService.getUserRank(gameId, userId, seasonId));
    }

    @PostMapping("/update")
    @Operation(summary = "更新我的得分排名")
    public Result<Void> update(@RequestParam Long gameId,
                               @RequestParam Integer score,
                               @RequestParam(required = false) Long seasonId,
                               @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        leaderboardService.updateRank(gameId, userId, score, seasonId);
        return Result.success();
    }
}
