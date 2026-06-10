package com.langtou.message.service.impl;

import com.langtou.message.entity.Message;
import com.langtou.message.mapper.MessageMapper;
import com.langtou.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public Message sendMessage(Long senderId, Long receiverId, Integer messageType, String content) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);
        log.info("消息发送成功: senderId={}, receiverId={}", senderId, receiverId);
        return message;
    }

    @Override
    public List<Message> getInbox(Long receiverId) {
        return messageMapper.selectByReceiverId(receiverId);
    }

    @Override
    public List<Message> getConversation(Long userId, Long targetId) {
        return messageMapper.selectConversation(userId, targetId);
    }

    @Override
    public Long getUnreadCount(Long receiverId) {
        return messageMapper.countUnread(receiverId);
    }

    @Override
    public void markAsRead(Long receiverId, Long senderId) {
        messageMapper.markAsRead(receiverId, senderId);
        log.info("消息标记已读: receiverId={}, senderId={}", receiverId, senderId);
    }
}
