package com.langtou.game.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.game.dto.GameSessionCreateRequest;
import com.langtou.game.dto.GameSessionResponse;
import com.langtou.game.service.GameSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game/sessions")
@RequiredArgsConstructor
@Tag(name = "游戏对局", description = "对局创建、加入、离开、开始、结束")
@SecurityRequirement(name = "bearer-jwt")
public class GameSessionController {

    private final GameSessionService gameSessionService;

    @PostMapping
    @Operation(summary = "创建对局")
    public Result<GameSessionResponse> create(@Valid @RequestBody GameSessionCreateRequest request,
                                               @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(gameSessionService.createSession(request, userId));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "查询对局详情")
    public Result<GameSessionResponse> get(@PathVariable Long sessionId) {
        return Result.success(gameSessionService.getSession(sessionId));
    }

    @PostMapping("/{sessionId}/join")
    @Operation(summary = "加入对局")
    public Result<GameSessionResponse> join(@PathVariable Long sessionId,
                                             @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(gameSessionService.joinSession(sessionId, userId));
    }

    @PostMapping("/{sessionId}/leave")
    @Operation(summary = "离开对局")
    public Result<GameSessionResponse> leave(@PathVariable Long sessionId,
                                              @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(gameSessionService.leaveSession(sessionId, userId));
    }

    @PostMapping("/{sessionId}/start")
    @Operation(summary = "开始对局")
    public Result<GameSessionResponse> start(@PathVariable Long sessionId,
                                              @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(gameSessionService.startSession(sessionId, userId));
    }

    @PostMapping("/{sessionId}/end")
    @Operation(summary = "结束对局")
    public Result<GameSessionResponse> end(@PathVariable Long sessionId,
                                            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        return Result.success(gameSessionService.endSession(sessionId, userId));
    }
}
