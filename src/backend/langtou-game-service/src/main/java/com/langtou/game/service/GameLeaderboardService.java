package com.langtou.game.service;

import com.langtou.game.dto.GameLeaderboardVO;

import java.util.List;

public interface GameLeaderboardService {

    List<GameLeaderboardVO> getLeaderboard(Long gameId, Long seasonId, int limit);

    GameLeaderboardVO getUserRank(Long gameId, Long userId, Long seasonId);

    void updateRank(Long gameId, Long userId, Integer score, Long seasonId);
}
