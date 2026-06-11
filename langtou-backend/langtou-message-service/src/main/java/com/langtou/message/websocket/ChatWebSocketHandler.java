package com.langtou.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.message.dto.MessageSendDTO;
import com.langtou.message.entity.Message;
import com.langtou.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 聊天WebSocket处理器，处理私信消息的实时推送
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理私信发送
     * 客户端发送到 /app/chat/send/{receiverId}
     */
    @MessageMapping("/chat/send/{receiverId}")
    public void handleChatMessage(@DestinationVariable Long receiverId, String payload) {
        try {
            // TODO: 从消息头中获取发送者userId，MVP阶段暂时从payload解析
            MessageSendDTO dto = objectMapper.readValue(payload, MessageSendDTO.class);
            Message message = messageService.sendMessage(dto.getSenderId(), dto);

            // 推送给接收者
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/chat",
                    message
            );

            // 同时推送给发送者自己（用于消息确认）
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.getSenderId()),
                    "/queue/chat",
                    message
            );

            log.info("私信推送成功: senderId={}, receiverId={}", dto.getSenderId(), receiverId);
        } catch (Exception e) {
            log.error("处理私信消息失败: receiverId={}, error={}", receiverId, e.getMessage(), e);
        }
    }
}
