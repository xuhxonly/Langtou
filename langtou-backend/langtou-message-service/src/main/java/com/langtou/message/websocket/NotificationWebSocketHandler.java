package com.langtou.message.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知WebSocket处理器，处理通知的实时推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 向指定用户推送通知
     * @param userId 目标用户ID
     * @param notification 通知数据
     */
    public void pushNotification(Long userId, Map<String, Object> notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notification",
                    notification
            );
            log.info("通知推送成功: userId={}, type={}", userId, notification.get("type"));
        } catch (Exception e) {
            log.error("通知推送失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 向指定用户推送未读数量更新
     * @param userId 目标用户ID
     * @param unreadCount 未读数量
     */
    public void pushUnreadCount(Long userId, Long unreadCount) {
        try {
            Map<String, Object> data = Map.of(
                    "type", "unread_count_update",
                    "unreadCount", unreadCount
            );
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notification",
                    data
            );
        } catch (Exception e) {
            log.error("未读数量推送失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
