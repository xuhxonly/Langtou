package com.langtou.interact.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {

    private String content;

    /**
     * 回复的目标用户ID
     */
    private Long replyUserId;

    /**
     * 回复的目标评论ID
     */
    private Long replyTo;
}
