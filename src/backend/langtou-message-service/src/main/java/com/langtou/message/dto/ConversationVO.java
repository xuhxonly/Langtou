package com.langtou.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVO {

    /**
     * 对方用户ID
     */
    private Long targetUserId;

    /**
     * 对方用户名
     */
    private String targetUsername;

    /**
     * 对方头像
     */
    private String targetAvatar;

    /**
     * 最新消息内容
     */
    private String lastMessage;

    /**
     * 最新消息类型
     */
    private Integer lastMessageType;

    /**
     * 最新消息时间
     */
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数
     */
    private Long unreadCount;
}
