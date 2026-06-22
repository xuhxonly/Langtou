﻿﻿﻿﻿﻿package com.langtou.game.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GameSessionCreateRequest;
import com.langtou.game.dto.GameSessionResponse;
import com.langtou.game.enums.SessionStatus;
import com.langtou.game.entity.GameSession;
import com.langtou.game.entity.GameSessionPlayer;
import com.langtou.game.mapper.GameSessionMapper;
import com.langtou.game.mapper.GameSessionPlayerMapper;
import com.langtou.game.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

    private static final String REDIS_KEY_PREFIX = "game:session:";
    private static final long REDIS_TTL_MINUTES = 30;

    private final GameSessionMapper gameSessionMapper;
    private final GameSessionPlayerMapper gameSessionPlayerMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameSessionResponse createSession(GameSessionCreateRequest request, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        GameSession session = new GameSession();
        session.setGameId(request.getGameId());
        session.setRoomId(request.getRoomId() == null
                ? UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : request.getRoomId());
        session.setHostUserId(userId);
        session.setStatus(SessionStatus.WAITING.name());
        session.setMaxPlayers(request.getMaxPlayers() == null ? 8 : request.getMaxPlayers());
        session.setCurrentPlayers(1);
        gameSessionMapper.insert(session);

        GameSessionPlayer player = new GameSessionPlayer();
        player.setSessionId(session.getId());
        player.setUserId(userId);
        player.setJoinedAt(LocalDateTime.now());
        gameSessionPlayerMapper.insert(player);

        evictCache(session.getId());
        return toResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameSessionResponse joinSession(Long sessionId, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("对局不存在");
        }
        if (!SessionStatus.WAITING.name().equals(session.getStatus())) {
            throw new BusinessException("对局当前不可加入");
        }

        int joined = gameSessionPlayerMapper.countBySessionAndUser(sessionId, userId);
        if (joined > 0) {
            throw new BusinessException("玩家已加入该对局");
        }

        int affected = gameSessionMapper.incrementCurrentPlayersIfWaiting(sessionId);
        if (affected <= 0) {
            throw new BusinessException("对局已满员或状态已变更，请重试");
        }

        GameSessionPlayer player = new GameSessionPlayer();
        player.setSessionId(sessionId);
        player.setUserId(userId);
        player.setJoinedAt(LocalDateTime.now());
        gameSessionPlayerMapper.insert(player);

        session.setCurrentPlayers((session.getCurrentPlayers() == null ? 0 : session.getCurrentPlayers()) + 1);
        evictCache(sessionId);
        return toResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameSessionResponse leaveSession(Long sessionId, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("对局不存在");
        }

        SessionStatus current = resolveStatus(session.getStatus());
        if (current == SessionStatus.FINISHED || current == SessionStatus.CANCELLED) {
            throw new BusinessException("对局已结束或已取消，无法离开");
        }

        gameSessionMapper.decrementCurrentPlayers(sessionId);
        int currentPlayers = (session.getCurrentPlayers() == null ? 0 : session.getCurrentPlayers()) - 1;
        session.setCurrentPlayers(Math.max(0, currentPlayers));

        if (session.getHostUserId() != null && session.getHostUserId().equals(userId)) {
            session.setStatus(SessionStatus.CANCELLED.name());
            session.setEndedAt(LocalDateTime.now());
        }
        gameSessionMapper.updateById(session);
        evictCache(sessionId);
        return toResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameSessionResponse startSession(Long sessionId, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("对局不存在");
        }
        if (!session.getHostUserId().equals(userId)) {
            throw new BusinessException("只有房主可以开始对局");
        }
        SessionStatus current = resolveStatus(session.getStatus());
        if (current != SessionStatus.WAITING) {
            throw new BusinessException("非法状态流转：当前状态为 " + current + "，无法开始对局");
        }
        session.setStatus(SessionStatus.IN_PROGRESS.name());
        session.setStartedAt(LocalDateTime.now());
        gameSessionMapper.updateById(session);
        evictCache(sessionId);
        return toResponse(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GameSessionResponse endSession(Long sessionId, Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("对局不存在");
        }
        boolean isHost = session.getHostUserId() != null && session.getHostUserId().equals(userId);
        boolean isAdmin = isAdmin(userId);
        if (!isHost && !isAdmin) {
            throw new BusinessException("只有房主或管理员可以结束对局");
        }
        SessionStatus current = resolveStatus(session.getStatus());
        if (current != SessionStatus.IN_PROGRESS) {
            throw new BusinessException("非法状态流转：当前状态为 " + current + "，无法结束对局");
        }
        session.setStatus(SessionStatus.FINISHED.name());
        session.setEndedAt(LocalDateTime.now());
        gameSessionMapper.updateById(session);
        evictCache(sessionId);
        return toResponse(session);
    }

    @Override
    public GameSessionResponse getSession(Long sessionId) {
        if (sessionId == null) {
            throw new BusinessException("对局ID不能为空");
        }
        String cacheKey = REDIS_KEY_PREFIX + sessionId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                GameSessionResponse response = objectMapper.readValue(cached, GameSessionResponse.class);
                if (response != null) {
                    return response;
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("解析对局缓存失败，sessionId={}", sessionId, e);
        }

        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("对局不存在");
        }
        GameSessionResponse response = toResponse(session);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("写入对局缓存失败，sessionId={}", sessionId, e);
        }
        return response;
    }

    private GameSessionResponse toResponse(GameSession session) {
        if (session == null) {
            return null;
        }
        GameSessionResponse response = new GameSessionResponse();
        response.setId(session.getId());
        response.setGameId(session.getGameId());
        response.setRoomId(session.getRoomId());
        response.setHostUserId(session.getHostUserId());
        response.setStatus(session.getStatus());
        response.setStartedAt(session.getStartedAt());
        response.setEndedAt(session.getEndedAt());
        response.setMaxPlayers(session.getMaxPlayers());
        response.setCurrentPlayers(session.getCurrentPlayers());
        return response;
    }

    private void evictCache(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(REDIS_KEY_PREFIX + sessionId);
        } catch (Exception e) {
            log.warn("删除对局缓存失败，sessionId={}", sessionId, e);
        }
    }

    private SessionStatus resolveStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return SessionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isAdmin(Long userId) {
        return false;
    }
}