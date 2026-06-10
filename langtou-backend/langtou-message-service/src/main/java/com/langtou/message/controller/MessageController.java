package com.langtou.message.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.message.entity.Message;
import com.langtou.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public Result<Message> sendMessage(@RequestBody Map<String, Object> params,
                                       @RequestHeader(CommonConstants.REQUEST_USER_ID) Long senderId) {
        Long receiverId = Long.valueOf(params.get("receiverId").toString());
        Integer messageType = params.containsKey("messageType") ? Integer.valueOf(params.get("messageType").toString()) : 1;
        String content = params.get("content").toString();
        Message message = messageService.sendMessage(senderId, receiverId, messageType, content);
        return Result.success("发送成功", message);
    }

    @GetMapping("/inbox")
    public Result<List<Message>> getInbox(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long receiverId) {
        List<Message> messages = messageService.getInbox(receiverId);
        return Result.success(messages);
    }

    @GetMapping("/conversation/{targetId}")
    public Result<List<Message>> getConversation(@PathVariable Long targetId,
                                                  @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<Message> messages = messageService.getConversation(userId, targetId);
        return Result.success(messages);
    }

    @GetMapping("/unread/count")
    public Result<Long> getUnreadCount(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long receiverId) {
        Long count = messageService.getUnreadCount(receiverId);
        return Result.success(count);
    }

    @PostMapping("/read/{senderId}")
    public Result<Void> markAsRead(@PathVariable Long senderId,
                                   @RequestHeader(CommonConstants.REQUEST_USER_ID) Long receiverId) {
        messageService.markAsRead(receiverId, senderId);
        return Result.success("标记已读成功");
    }
}
