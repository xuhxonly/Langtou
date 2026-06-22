package com.langtou.game.service;

import com.langtou.game.dto.GameSessionCreateRequest;
import com.langtou.game.dto.GameSessionResponse;

public interface GameSessionService {

    GameSessionResponse createSession(GameSessionCreateRequest request, Long userId);

    GameSessionResponse joinSession(Long sessionId, Long userId);

    GameSessionResponse leaveSession(Long sessionId, Long userId);

    GameSessionResponse startSession(Long sessionId, Long userId);

    GameSessionResponse endSession(Long sessionId, Long userId);

    GameSessionResponse getSession(Long sessionId);
}
