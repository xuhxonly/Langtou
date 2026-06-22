package com.langtou.message.dto;

import lombok.Data;

@Data
public class MessageSendDTO {

    private Long receiverId;

    /**
     * 消息类型: 1-文本, 2-图片
     */
    private Integer messageType;

    private String content;
}
