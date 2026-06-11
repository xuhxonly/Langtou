package com.langtou.interact.dto;

import com.langtou.interact.entity.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {

    private Long id;

    private Long userId;

    private String username;

    private String avatar;

    private Long contentId;

    private String content;

    private Long parentId;

    private Long replyUserId;

    private String replyUsername;

    private Integer likeCount;

    private Boolean liked;

    private LocalDateTime createTime;

    /**
     * 子评论列表（回复）
     */
    private List<CommentVO> replies;

    public static CommentVO fromComment(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setContentId(comment.getContentId());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setReplyUserId(comment.getReplyUserId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
}
