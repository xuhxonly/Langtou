package com.langtou.message.service;

import com.langtou.message.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long senderId, Long receiverId, Integer messageType, String content);

    List<Message> getInbox(Long receiverId);

    List<Message> getConversation(Long userId, Long targetId);

    Long getUnreadCount(Long receiverId);

    void markAsRead(Long receiverId, Long senderId);
}
