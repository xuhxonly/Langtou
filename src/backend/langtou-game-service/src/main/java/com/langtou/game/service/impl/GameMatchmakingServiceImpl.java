package com.langtou.game.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GameMatchmakingRequest;
import com.langtou.game.dto.GameMatchmakingResponse;
import com.langtou.game.entity.GameMatchmaking;
import com.langtou.game.mapper.GameMatchmakingMapper;
import com.langtou.game.service.GameMatchmakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameMatchmakingServiceImpl implements GameMatchmakingService {

    private final GameMatchmakingMapper matchmakingMapper;

    @Override
    public GameMatchmakingResponse submitMatchmaking(GameMatchmakingRequest request, Long userId) {
        GameMatchmaking matchmaking = new GameMatchmaking();
        matchmaking.setUserId(userId);
        matchmaking.setGameId(request.getGameId());
        matchmaking.setQueueType(request.getQueueType() == null ? "RANKED" : request.getQueueType());
        matchmaking.setMmr(request.getCurrentMmr() == null ? calculateMmr(userId, request.getGameId()) : request.getCurrentMmr());
        matchmaking.setStatus("QUEUED");
        matchmaking.setExpectedWaitTime(30);
        matchmakingMapper.insert(matchmaking);
        return toResponse(matchmaking);
    }

    @Override
    public void cancelMatchmaking(Long matchmakingId, Long userId) {
        GameMatchmaking matchmaking = matchmakingMapper.selectById(matchmakingId);
        if (matchmaking == null || !matchmaking.getUserId().equals(userId)) {
            throw new BusinessException("匹配记录不存在");
        }
        matchmaking.setStatus("CANCELLED");
        matchmakingMapper.updateById(matchmaking);
    }

    @Override
    public Integer calculateMmr(Long userId, Long gameId) {
        return 1000;
    }

    private GameMatchmakingResponse toResponse(GameMatchmaking matchmaking) {
        GameMatchmakingResponse response = new GameMatchmakingResponse();
        response.setId(matchmaking.getId());
        response.setUserId(matchmaking.getUserId());
        response.setGameId(matchmaking.getGameId());
        response.setMmr(matchmaking.getMmr());
        response.setQueueType(matchmaking.getQueueType());
        response.setStatus(matchmaking.getStatus());
        response.setExpectedWaitTime(matchmaking.getExpectedWaitTime());
        response.setCreatedAt(matchmaking.getCreatedAt());
        return response;
    }
}
