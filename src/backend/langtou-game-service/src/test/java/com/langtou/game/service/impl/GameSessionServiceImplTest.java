package com.langtou.game.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GameSessionCreateRequest;
import com.langtou.game.dto.GameSessionResponse;
import com.langtou.game.entity.GameSession;
import com.langtou.game.entity.GameSessionPlayer;
import com.langtou.game.mapper.GameSessionMapper;
import com.langtou.game.mapper.GameSessionPlayerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameSessionServiceImpl 单元测试")
class GameSessionServiceImplTest {

    @Mock
    private GameSessionMapper gameSessionMapper;

    @Mock
    private GameSessionPlayerMapper gameSessionPlayerMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GameSessionServiceImpl gameSessionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("createSession 成功创建对局")
    void createSession_success() {
        GameSessionCreateRequest request = new GameSessionCreateRequest();
        request.setGameId(1L);
        request.setMaxPlayers(8);

        GameSession session = new GameSession();
        session.setId(100L);
        session.setGameId(1L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");
        session.setMaxPlayers(8);
        session.setCurrentPlayers(1);

        when(gameSessionMapper.insert(any(GameSession.class))).thenAnswer(invocation -> {
            GameSession s = invocation.getArgument(0);
            s.setId(100L);
            return 1;
        });

        GameSessionResponse response = gameSessionService.createSession(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getGameId()).isEqualTo(1L);
        assertThat(response.getHostUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getCurrentPlayers()).isEqualTo(1);
        verify(gameSessionMapper).insert(any(GameSession.class));
        verify(gameSessionPlayerMapper).insert(any(GameSessionPlayer.class));
        verify(stringRedisTemplate).delete(eq("game:session:100"));
    }

    @Test
    @DisplayName("createSession 用户ID为空抛出异常")
    void createSession_nullUserId() {
        GameSessionCreateRequest request = new GameSessionCreateRequest();
        request.setGameId(1L);

        assertThatThrownBy(() -> gameSessionService.createSession(request, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    @DisplayName("joinSession 并发加入 - 乐观锁验证成功")
    void joinSession_withOptimisticLock_success() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setGameId(1L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");
        session.setMaxPlayers(8);
        session.setCurrentPlayers(1);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);
        when(gameSessionPlayerMapper.countBySessionAndUser(100L, 2L)).thenReturn(0);
        when(gameSessionMapper.incrementCurrentPlayersIfWaiting(100L)).thenReturn(1);

        GameSessionResponse response = gameSessionService.joinSession(100L, 2L);

        assertThat(response.getStatus()).isEqualTo("WAITING");
        assertThat(response.getCurrentPlayers()).isEqualTo(2);
        verify(gameSessionMapper).incrementCurrentPlayersIfWaiting(100L);
        verify(gameSessionPlayerMapper).insert(any(GameSessionPlayer.class));
    }

    @Test
    @DisplayName("joinSession 乐观锁冲突抛异常")
    void joinSession_optimisticLockConflict() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(7);
        session.setMaxPlayers(8);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);
        when(gameSessionPlayerMapper.countBySessionAndUser(100L, 2L)).thenReturn(0);
        when(gameSessionMapper.incrementCurrentPlayersIfWaiting(100L)).thenReturn(0);

        assertThatThrownBy(() -> gameSessionService.joinSession(100L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对局已满员或状态已变更");
    }

    @Test
    @DisplayName("joinSession 重复加入防护")
    void joinSession_duplicateJoinBlocked() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(1);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);
        when(gameSessionPlayerMapper.countBySessionAndUser(100L, 1L)).thenReturn(1);

        assertThatThrownBy(() -> gameSessionService.joinSession(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("玩家已加入该对局");
        verify(gameSessionPlayerMapper, never()).insert(any(GameSessionPlayer.class));
    }

    @Test
    @DisplayName("joinSession 非WAITING状态不可加入")
    void joinSession_notWaitingStatus() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setStatus("IN_PROGRESS");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.joinSession(100L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对局当前不可加入");
    }

    @Test
    @DisplayName("startSession 只有房主能开始")
    void startSession_byHost_success() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(2);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.startSession(100L, 1L);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        verify(gameSessionMapper).updateById(session);
    }

    @Test
    @DisplayName("startSession 非房主抛出异常")
    void startSession_notHost() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.startSession(100L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有房主可以开始对局");
    }

    @Test
    @DisplayName("startSession 非法状态流转")
    void startSession_illegalStatusFlow() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("IN_PROGRESS");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.startSession(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态流转");
    }

    @Test
    @DisplayName("endSession 房主可结束")
    void endSession_byHost_success() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("IN_PROGRESS");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.endSession(100L, 1L);

        assertThat(response.getStatus()).isEqualTo("FINISHED");
        verify(gameSessionMapper).updateById(session);
    }

    @Test
    @DisplayName("endSession 管理员可结束")
    void endSession_byAdmin_success() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("IN_PROGRESS");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.endSession(100L, 999L);

        assertThat(response.getStatus()).isEqualTo("FINISHED");
    }

    @Test
    @DisplayName("endSession 非房主/管理员不可结束")
    void endSession_forbidden() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("IN_PROGRESS");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.endSession(100L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有房主或管理员可以结束对局");
    }

    @Test
    @DisplayName("endSession 非法状态流转")
    void endSession_illegalStatusFlow() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.endSession(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态流转");
    }

    @Test
    @DisplayName("leaveSession 房主离开时对局取消")
    void leaveSession_hostCancelSession() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(2);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.leaveSession(100L, 1L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getEndedAt()).isNotNull();
        verify(gameSessionMapper).updateById(session);
    }

    @Test
    @DisplayName("leaveSession 普通玩家离开不取消")
    void leaveSession_playerLeaveNotCancel() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setHostUserId(1L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(2);

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.leaveSession(100L, 2L);

        assertThat(response.getStatus()).isEqualTo("WAITING");
    }

    @Test
    @DisplayName("leaveSession 已结束对局无法离开")
    void leaveSession_finishedCannotLeave() {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setStatus("FINISHED");

        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        assertThatThrownBy(() -> gameSessionService.leaveSession(100L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对局已结束或已取消");
    }

    @Test
    @DisplayName("getSession 缓存命中")
    void getSession_cacheHit() throws Exception {
        GameSessionResponse cachedResp = new GameSessionResponse();
        cachedResp.setId(100L);
        cachedResp.setGameId(1L);
        cachedResp.setStatus("WAITING");
        cachedResp.setCurrentPlayers(2);

        String cachedJson = objectMapper.writeValueAsString(cachedResp);
        when(valueOperations.get("game:session:100")).thenReturn(cachedJson);

        GameSessionResponse response = gameSessionService.getSession(100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("WAITING");
        verify(gameSessionMapper, never()).selectById(anyLong());
    }

    @Test
    @DisplayName("getSession 缓存未命中回源DB")
    void getSession_cacheMiss() throws Exception {
        GameSession session = new GameSession();
        session.setId(100L);
        session.setGameId(1L);
        session.setStatus("WAITING");
        session.setCurrentPlayers(2);

        when(valueOperations.get("game:session:100")).thenReturn(null);
        when(gameSessionMapper.selectById(100L)).thenReturn(session);

        GameSessionResponse response = gameSessionService.getSession(100L);

        assertThat(response.getId()).isEqualTo(100L);
        verify(gameSessionMapper).selectById(100L);
        verify(valueOperations).set(eq("game:session:100"), anyString(), anyLong(), any());
    }
}
