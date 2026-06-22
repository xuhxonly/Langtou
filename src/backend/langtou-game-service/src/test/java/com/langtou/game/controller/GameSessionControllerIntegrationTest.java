package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.game.GameServiceApplication;
import com.langtou.game.config.TestRedisConfiguration;
import com.langtou.game.dto.GameSessionCreateRequest;
import com.langtou.game.entity.GameSession;
import com.langtou.game.mapper.GameSessionMapper;
import com.langtou.game.mapper.GameSessionPlayerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GameServiceApplication.class)
@AutoConfigureMockMvc
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("GameSessionController 集成测试")
class GameSessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameSessionMapper gameSessionMapper;

    @Autowired
    private GameSessionPlayerMapper gameSessionPlayerMapper;

    private static Long createdSessionId;

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/game/sessions 创建对局")
    void createSession_success() throws Exception {
        GameSessionCreateRequest request = new GameSessionCreateRequest();
        request.setGameId(1L);
        request.setMaxPlayers(6);

        MvcResult result = mockMvc.perform(post("/api/v1/game/sessions")
                        .header(CommonConstants.REQUEST_USER_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.hostUserId").value(1))
                .andExpect(jsonPath("$.data.currentPlayers").value(1))
                .andExpect(jsonPath("$.data.id").value(notNullValue()))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        createdSessionId = objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/v1/game/sessions/{id}/join 加入对局")
    void joinSession_success() throws Exception {
        mockMvc.perform(post("/api/v1/game/sessions/{id}/join", createdSessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentPlayers").value(2));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v1/game/sessions/{id}/join 重复加入被拒绝")
    void joinSession_duplicate() throws Exception {
        mockMvc.perform(post("/api/v1/game/sessions/{id}/join", createdSessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/v1/game/sessions/{id}/start 非房主不能开始")
    void startSession_forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/game/sessions/{id}/start", createdSessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/game/sessions/{id}/start 房主开始对局成功")
    void startSession_byHost() throws Exception {
        mockMvc.perform(post("/api/v1/game/sessions/{id}/start", createdSessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/game/sessions/{id} 查询对局详情")
    void getSession_success() throws Exception {
        mockMvc.perform(get("/api/v1/game/sessions/{id}", createdSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(createdSessionId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/v1/game/sessions/{id}/end 结束对局")
    void endSession_success() throws Exception {
        mockMvc.perform(post("/api/v1/game/sessions/{id}/end", createdSessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("FINISHED"));
    }

    @Test
    @Order(8)
    @DisplayName("完整生命周期 - 新建对局走一遍 create->join->start->end")
    void fullLifecycle() throws Exception {
        GameSessionCreateRequest request = new GameSessionCreateRequest();
        request.setGameId(2L);
        request.setMaxPlayers(4);

        MvcResult createResult = mockMvc.perform(post("/api/v1/game/sessions")
                        .header(CommonConstants.REQUEST_USER_ID, "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andReturn();

        Long sessionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/game/sessions/{id}/join", sessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentPlayers").value(2));

        mockMvc.perform(post("/api/v1/game/sessions/{id}/start", sessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/game/sessions/{id}/end", sessionId)
                        .header(CommonConstants.REQUEST_USER_ID, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FINISHED"));

        GameSession finished = gameSessionMapper.selectById(sessionId);
        assert finished != null;
        assert "FINISHED".equals(finished.getStatus());
    }
}
