package com.langtou.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.dto.CommentCreateDTO;
import com.langtou.interact.dto.CommentVO;
import com.langtou.interact.entity.Comment;
import com.langtou.interact.entity.LikeRecord;
import com.langtou.interact.entity.ShareRecord;
import com.langtou.interact.mapper.CommentMapper;
import com.langtou.interact.mapper.LikeRecordMapper;
import com.langtou.interact.mapper.ShareRecordMapper;
import com.langtou.interact.service.InteractService;
import com.langtou.interact.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractServiceImpl implements InteractService {

    private final LikeRecordMapper likeRecordMapper;
    private final CommentMapper commentMapper;
    private final ShareService shareService;
    private final com.langtou.common.client.ContentClient contentClient;

    // ========== 点赞 ==========

    @Override
    public void like(Long userId, Long contentId) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContent(userId, contentId);
        if (exist != null) {
            throw new BusinessException(ResultCode.ALREADY_LIKED);
        }
        LikeRecord record = new LikeRecord();
        record.setUserId(userId);
        record.setTargetId(contentId);
        record.setTargetType("note");
        likeRecordMapper.insert(record);
        // 同步更新笔记点赞数
        try {
            contentClient.incrementLikeCount(contentId);
        } catch (Exception e) {
            log.warn("同步点赞计数失败: contentId={}, error={}", contentId, e.getMessage());
        }
        log.info("点赞成功: userId={}, contentId={}", userId, contentId);
    }

    @Override
    public void unlike(Long userId, Long contentId) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContent(userId, contentId);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_LIKED);
        }
        likeRecordMapper.deleteById(exist.getId());
        // 同步更新笔记点赞数
        try {
            contentClient.decrementLikeCount(contentId);
        } catch (Exception e) {
            log.warn("同步取消点赞计数失败: contentId={}, error={}", contentId, e.getMessage());
        }
        log.info("取消点赞成功: userId={}, contentId={}", userId, contentId);
    }

    @Override
    public void likeByType(Long userId, Long contentId, String targetType) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContentAndType(userId, contentId, targetType);
        if (exist != null) {
            throw new BusinessException(ResultCode.ALREADY_LIKED);
        }
        LikeRecord record = new LikeRecord();
        record.setUserId(userId);
        record.setTargetId(contentId);
        record.setTargetType(targetType);
        likeRecordMapper.insert(record);
        // 同步更新笔记点赞数（仅笔记类型）
        if ("note".equals(targetType)) {
            try {
                contentClient.incrementLikeCount(contentId);
            } catch (Exception e) {
                log.warn("同步点赞计数失败: contentId={}, error={}", contentId, e.getMessage());
            }
        }
        log.info("点赞成功: userId={}, contentId={}, targetType={}", userId, contentId, targetType);
    }

    @Override
    public void unlikeByType(Long userId, Long contentId, String targetType) {
        LikeRecord exist = likeRecordMapper.selectByUserAndContentAndType(userId, contentId, targetType);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_LIKED);
        }
        likeRecordMapper.deleteById(exist.getId());
        // 同步更新笔记点赞数
        try {
            contentClient.decrementLikeCount(contentId);
        } catch (Exception e) {
            log.warn("同步取消点赞计数失败: contentId={}, error={}", contentId, e.getMessage());
        }
        log.info("取消点赞成功: userId={}, contentId={}, targetType={}", userId, contentId, targetType);
    }

    @Override
    public boolean hasLiked(Long userId, Long contentId) {
        return likeRecordMapper.selectByUserAndContent(userId, contentId) != null;
    }

    @Override
    public boolean hasLikedByType(Long userId, Long contentId, String targetType) {
        return likeRecordMapper.selectByUserAndContentAndType(userId, contentId, targetType) != null;
    }

    @Override
    public Long getLikeCount(Long contentId) {
        return likeRecordMapper.countByContentId(contentId);
    }

    // ========== 评论 ==========

    @Override
    public Comment comment(Long userId, Long contentId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setContentId(contentId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setLikeCount(0);
        commentMapper.insert(comment);
        // 同步更新笔记评论数
        try {
            contentClient.incrementCommentCount(contentId);
        } catch (Exception e) {
            log.warn("同步评论计数失败: contentId={}, error={}", contentId, e.getMessage());
        }
        log.info("评论成功: userId={}, contentId={}", userId, contentId);
        return comment;
    }

    @Override
    public Comment replyComment(Long userId, Long commentId, CommentCreateDTO dto) {
        Comment parentComment = commentMapper.selectById(commentId);
        if (parentComment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        Comment reply = new Comment();
        reply.setUserId(userId);
        reply.setContentId(parentComment.getContentId());
        reply.setContent(dto.getContent());
        reply.setParentId(parentComment.getId());
        reply.setReplyUserId(dto.getReplyUserId());
        reply.setReplyTo(commentId);
        reply.setLikeCount(0);
        commentMapper.insert(reply);
        // 同步更新笔记评论数
        try {
            contentClient.incrementCommentCount(parentComment.getContentId());
        } catch (Exception e) {
            log.warn("同步评论计数失败: contentId={}, error={}", parentComment.getContentId(), e.getMessage());
        }
        log.info("回复评论成功: userId={}, commentId={}", userId, commentId);
        return reply;
    }

    @Override
    public void likeComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
        }
        likeByType(userId, commentId, "comment");
        // 更新评论点赞数
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(comment);
        log.info("点赞评论成功: userId={}, commentId={}", userId, commentId);
    }

    @Override
    public List<Comment> getComments(Long contentId) {
        return commentMapper.selectRootCommentsByContentId(contentId);
    }

    @Override
    public Page<CommentVO> getCommentsWithTree(Long contentId, Long userId, long current, long size) {
        // 1. 分页查询根评论
        Page<Comment> page = new Page<>(current, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getContentId, contentId)
               .isNull(Comment::getParentId)
               .orderByDesc(Comment::getCreateTime);
        Page<Comment> rootPage = commentMapper.selectPage(page, wrapper);

        if (rootPage.getRecords().isEmpty()) {
            return new Page<>(current, size, 0);
        }

        // 2. 获取所有根评论ID
        List<Long> rootIds = rootPage.getRecords().stream()
                .map(Comment::getId)
                .collect(Collectors.toList());

        // 3. 批量查询子评论
        LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(Comment::getContentId, contentId)
                    .in(Comment::getParentId, rootIds)
                    .orderByAsc(Comment::getCreateTime);
        List<Comment> allReplies = commentMapper.selectList(replyWrapper);

        // 4. 按parentId分组
        Map<Long, List<Comment>> replyMap = allReplies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 5. 查询当前用户对这些评论的点赞状态
        Set<Long> likedCommentIds = getLikedCommentIds(userId, rootIds, allReplies);

        // 6. 组装树形结构
        List<CommentVO> voList = new ArrayList<>();
        for (Comment root : rootPage.getRecords()) {
            CommentVO rootVO = CommentVO.fromComment(root);
            rootVO.setLiked(likedCommentIds.contains(root.getId()));

            List<Comment> replies = replyMap.getOrDefault(root.getId(), new ArrayList<>());
            List<CommentVO> replyVOList = new ArrayList<>();
            for (Comment reply : replies) {
                CommentVO replyVO = CommentVO.fromComment(reply);
                replyVO.setLiked(likedCommentIds.contains(reply.getId()));
                replyVOList.add(replyVO);
            }
            rootVO.setReplies(replyVOList);
            voList.add(rootVO);
        }

        // 7. 构建分页结果
        Page<CommentVO> result = new Page<>(rootPage.getCurrent(), rootPage.getSize(), rootPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    /**
     * 获取当前用户点赞过的评论ID集合
     */
    private Set<Long> getLikedCommentIds(Long userId, List<Long> rootIds, List<Comment> replies) {
        if (userId == null) {
            return Set.of();
        }
        List<Long> allCommentIds = new ArrayList<>(rootIds);
        allCommentIds.addAll(replies.stream().map(Comment::getId).collect(Collectors.toList()));

        if (allCommentIds.isEmpty()) {
            return Set.of();
        }

        LambdaQueryWrapper<LikeRecord> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(LikeRecord::getUserId, userId)
                   .eq(LikeRecord::getTargetType, "comment")
                   .in(LikeRecord::getContentId, allCommentIds);
        List<LikeRecord> likeRecords = likeRecordMapper.selectList(likeWrapper);
        return likeRecords.stream()
                .map(LikeRecord::getContentId)
                .collect(Collectors.toSet());
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

    // ========== 转发 ==========

    @Override
    public ShareRecord share(Long userId, Long noteId, String shareType) {
        return shareService.share(userId, noteId, shareType);
    }

    @Override
    public String generateShareLink(Long noteId) {
        // 生成分享链接，MVP阶段使用本地链接格式
        return "https://langtou.com/note/" + noteId;
    }
}
