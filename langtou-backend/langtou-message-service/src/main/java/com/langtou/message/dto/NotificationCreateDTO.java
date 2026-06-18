package com.langtou.message.dto;

import lombok.Data;

@Data
public class NotificationCreateDTO {

    private Long userId;

    private String type;

    private Long sourceId;

    private String sourceType;

    private String content;

    private Long fromUserId;
}
