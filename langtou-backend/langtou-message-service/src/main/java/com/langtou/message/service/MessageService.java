package com.langtou.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.message.dto.ConversationVO;
import com.langtou.message.dto.MessageSendDTO;
import com.langtou.message.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long senderId, MessageSendDTO dto);

    List<Message> getInbox(Long receiverId);

    Page<Message> getConversation(Long userId, Long targetId, long current, long size);

    Long getUnreadCount(Long receiverId);

    void markAsRead(Long receiverId, Long senderId);

    List<ConversationVO> getConversations(Long userId);

    void deleteMessage(Long userId, Long messageId);
}
