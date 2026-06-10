package com.langtou.interact.service;

import com.langtou.interact.entity.Comment;

import java.util.List;

public interface InteractService {

    void like(Long userId, Long contentId);

    void unlike(Long userId, Long contentId);

    boolean hasLiked(Long userId, Long contentId);

    Long getLikeCount(Long contentId);

    Comment comment(Long userId, Long contentId, String content, Long parentId);

    List<Comment> getComments(Long contentId);

    void deleteComment(Long userId, Long commentId);
}
