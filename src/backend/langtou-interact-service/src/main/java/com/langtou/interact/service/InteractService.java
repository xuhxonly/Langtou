package com.langtou.interact.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.interact.dto.CommentCreateDTO;
import com.langtou.interact.dto.CommentVO;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.entity.ShareRecord;

import java.util.List;
import java.util.Map;

public interface InteractService {

    // ========== 点赞 ==========

    void like(Long userId, Long contentId);

    void unlike(Long userId, Long contentId);

    void likeByType(Long userId, Long contentId, String targetType);

    void unlikeByType(Long userId, Long contentId, String targetType);

    boolean hasLiked(Long userId, Long contentId);

    boolean hasLikedByType(Long userId, Long contentId, String targetType);

    Long getLikeCount(Long contentId);

    // ========== 评论 ==========

    Comment comment(Long userId, Long contentId, String content, Long parentId);

    Comment replyComment(Long userId, Long commentId, CommentCreateDTO dto);

    void likeComment(Long userId, Long commentId);

    List<Comment> getComments(Long contentId);

    Page<CommentVO> getCommentsWithTree(Long contentId, Long userId, long current, long size);

    void deleteComment(Long userId, Long commentId);

    // ========== 转发 ==========

    ShareRecord share(Long userId, Long noteId, String shareType);

    /**
     * 生成笔记分享链接
     */
    String generateShareLink(Long noteId);
}
