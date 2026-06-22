package com.langtou.game.service;

import com.langtou.game.dto.GameMatchmakingRequest;
import com.langtou.game.dto.GameMatchmakingResponse;

public interface GameMatchmakingService {

    GameMatchmakingResponse submitMatchmaking(GameMatchmakingRequest request, Long userId);

    void cancelMatchmaking(Long matchmakingId, Long userId);

    Integer calculateMmr(Long userId, Long gameId);
}
