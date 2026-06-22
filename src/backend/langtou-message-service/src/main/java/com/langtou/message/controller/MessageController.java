package com.langtou.message.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.message.dto.ConversationVO;
import com.langtou.message.dto.MessageSendDTO;
import com.langtou.message.entity.Message;
import com.langtou.message.service.MessageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "消息服务", description = "私信、会话、通知、推送等消息相关接口")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/messages/send")
    @Operation(summary = "发送私信", description = "向指定用户发送私信")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Message> sendMessage(@Valid @RequestBody MessageSendDTO dto,
                                       @RequestHeader(CommonConstants.REQUEST_USER_ID) Long senderId) {
        Message message = messageService.sendMessage(senderId, dto);
        return Result.success("发送成功", message);
    }

    @GetMapping("/messages/conversations")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话列表")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<List<ConversationVO>> getConversations(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<ConversationVO> conversations = messageService.getConversations(userId);
        return Result.success(conversations);
    }

    @GetMapping("/messages/conversation/{userId}")
    @Operation(summary = "获取会话消息", description = "获取与指定用户的会话历史消息")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<PageResult<Message>> getConversation(
            @Parameter(name = "userId", description = "对方用户ID", required = true) @PathVariable Long userId,
            @Parameter(name = "current", description = "当前页") @RequestParam(defaultValue = "1") long current,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") long size,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId) {
        Page<Message> page = messageService.getConversation(currentUserId, userId, current, size);
        return Result.success(PageResult.of(page));
    }

    @PutMapping("/messages/conversation/{userId}/read")
    @Operation(summary = "标记会话已读", description = "将会话中的所有消息标记为已读")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> markConversationAsRead(
            @Parameter(name = "userId", description = "对方用户ID", required = true) @PathVariable Long userId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId) {
        messageService.markAsRead(currentUserId, userId);
        return Result.success("标记已读成功");
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "删除消息", description = "删除指定的消息")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> deleteMessage(
            @Parameter(name = "messageId", description = "消息ID", required = true) @PathVariable Long messageId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        messageService.deleteMessage(userId, messageId);
        return Result.success("删除成功");
    }
}