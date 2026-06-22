package com.langtou.message.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;

    private Long userId;

    /**
     * 通知类型: LIKE, COMMENT, FOLLOW, COLLECT, SHARE, SYSTEM
     */
    private String type;

    private Long sourceId;

    private String sourceType;

    private String content;

    private Integer isRead;

    private LocalDateTime createTime;

    /**
     * 触发者用户ID
     */
    private Long fromUserId;

    /**
     * 触发者用户名
     */
    private String fromUsername;

    /**
     * 触发者头像
     */
    private String fromAvatar;
}
