package com.langtou.interact.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.entity.LikeRecord;
import com.langtou.interact.mapper.CommentMapper;
import com.langtou.interact.mapper.LikeRecordMapper;
import com.langtou.interact.service.InteractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final LikeRecordMapper likeRecordMapper;
    private final CommentMapper commentMapper;

    @Override
    public void like(Long userId, Long contentId) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContent(userId, contentId);
        if (exist != null) {
            throw new BusinessException(ResultCode.ALREADY_LIKED);
        }
        LikeRecord record = new LikeRecord();
        record.setUserId(userId);
        record.setContentId(contentId);
        likeRecordMapper.insert(record);
        log.info("点赞成功: userId={}, contentId={}", userId, contentId);
    }

    @Override
    public void unlike(Long userId, Long contentId) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContent(userId, contentId);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_LIKED);
        }
        likeRecordMapper.deleteById(exist.getId());
        log.info("取消点赞成功: userId={}, contentId={}", userId, contentId);
    }

    @Override
    public boolean hasLiked(Long userId, Long contentId) {
        return likeRecordMapper.selectByUserAndContent(userId, contentId) != null;
    }

    @Override
    public Long getLikeCount(Long contentId) {
        return likeRecordMapper.countByContentId(contentId);
    }

    @Override
    public Comment comment(Long userId, Long contentId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setContentId(contentId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        commentMapper.insert(comment);
        log.info("评论成功: userId={}, contentId={}", userId, contentId);
        return comment;
    }

    @Override
    public List<Comment> getComments(Long contentId) {
        return commentMapper.selectRootCommentsByContentId(contentId);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该评论");
        }
        commentMapper.deleteById(commentId);
        log.info("删除评论成功: commentId={}", commentId);
    }
}
