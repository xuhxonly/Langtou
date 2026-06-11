package com.langtou.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.message.dto.ConversationVO;
import com.langtou.message.dto.MessageSendDTO;
import com.langtou.message.entity.Message;
import com.langtou.message.mapper.MessageMapper;
import com.langtou.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    public Message sendMessage(Long senderId, MessageSendDTO dto) {
        if (senderId.equals(dto.getReceiverId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不能给自己发消息");
        }
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setMessageType(dto.getMessageType());
        message.setContent(dto.getContent());
        message.setIsRead(0);
        messageMapper.insert(message);
        log.info("消息发送成功: senderId={}, receiverId={}", senderId, dto.getReceiverId());
        return message;
    }

    @Override
    public List<Message> getInbox(Long receiverId) {
        return messageMapper.selectByReceiverId(receiverId);
    }

    @Override
    public Page<Message> getConversation(Long userId, Long targetId, long current, long size) {
        Page<Message> page = new Page<>(current, size);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .and(w1 -> w1.eq(Message::getSenderId, userId).eq(Message::getReceiverId, targetId))
                .or(w2 -> w2.eq(Message::getSenderId, targetId).eq(Message::getReceiverId, userId))
        ).orderByDesc(Message::getCreateTime);
        return messageMapper.selectPage(page, wrapper);
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

    @Override
    public List<ConversationVO> getConversations(Long userId) {
        // 1. 获取所有会话的对方用户ID
        List<Long> targetIds = messageMapper.selectConversationTargetIds(userId);
        if (targetIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 为每个会话构建VO
        List<ConversationVO> conversations = new ArrayList<>();
        for (Long targetId : targetIds) {
            ConversationVO vo = new ConversationVO();
            vo.setTargetUserId(targetId);

            // 查询最新消息
            Message latestMessage = messageMapper.selectLatestMessage(userId, targetId);
            if (latestMessage != null) {
                vo.setLastMessage(latestMessage.getContent());
                vo.setLastMessageType(latestMessage.getMessageType());
                vo.setLastMessageTime(latestMessage.getCreateTime());
            }

            // 查询未读数
            Long unreadCount = messageMapper.countUnreadFromSender(userId, targetId);
            vo.setUnreadCount(unreadCount != null ? unreadCount : 0L);

            conversations.add(vo);
        }

        // 3. 按最新消息时间降序排列
        conversations.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });

        return conversations;
    }

    @Override
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该消息");
        }
        messageMapper.deleteById(messageId);
        log.info("删除消息成功: userId={}, messageId={}", userId, messageId);
    }
}
