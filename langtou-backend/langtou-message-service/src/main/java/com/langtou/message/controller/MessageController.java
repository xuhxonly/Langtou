package com.langtou.message.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.utils.PageUtils;
import com.langtou.message.dto.ConversationVO;
import com.langtou.message.dto.MessageSendDTO;
import com.langtou.message.entity.Message;
import com.langtou.message.service.MessageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/messages/send")
    public Result<Message> sendMessage(@RequestBody MessageSendDTO dto,
                                       @RequestHeader(CommonConstants.REQUEST_USER_ID) Long senderId) {
        Message message = messageService.sendMessage(senderId, dto);
        return Result.success("发送成功", message);
    }

    @GetMapping("/messages/conversations")
    public Result<List<ConversationVO>> getConversations(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<ConversationVO> conversations = messageService.getConversations(userId);
        return Result.success(conversations);
    }

    @GetMapping("/messages/conversation/{userId}")
    public Result<PageUtils.PageResult<Message>> getConversation(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId) {
        Page<Message> page = messageService.getConversation(currentUserId, userId, current, size);
        return Result.success(PageUtils.PageResult.of(page));
    }

    @PutMapping("/messages/conversation/{userId}/read")
    public Result<Void> markConversationAsRead(@PathVariable Long userId,
                                                @RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId) {
        messageService.markAsRead(currentUserId, userId);
        return Result.success("标记已读成功");
    }

    @DeleteMapping("/messages/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        messageService.deleteMessage(userId, messageId);
        return Result.success("删除成功");
    }
}
