package com.langtou.quiz.controller;

import com.langtou.common.result.Result;
import com.langtou.quiz.dto.LeaderboardEntry;
import com.langtou.quiz.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/global")
    public Result<List<LeaderboardEntry>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(leaderboardService.getGlobalLeaderboard(limit));
    }

    @GetMapping("/quiz/{setId}")
    public Result<List<LeaderboardEntry>> getQuizLeaderboard(
            @PathVariable Long setId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(leaderboardService.getQuizLeaderboard(setId, limit));
    }

    @GetMapping("/friends")
    public Result<List<LeaderboardEntry>> getFriendLeaderboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(leaderboardService.getFriendLeaderboard(userId, limit));
    }
}
