package com.langtou.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.client.NotificationClient;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.entity.Collection;
import com.langtou.interact.mapper.CollectionMapper;
import com.langtou.interact.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final com.langtou.common.client.ContentClient contentClient;
    private final NotificationClient notificationClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void collect(Long userId, Long noteId) {
        // 频率限制：10秒内只能操作一次
        String rateLimitKey = "rate:interact:" + userId + ":collect";
        Boolean allowed = stringRedisTemplate.opsForValue()
                .setIfAbsent(rateLimitKey, "1", Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(allowed)) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }
        Collection exist = collectionMapper.selectByUserAndNote(userId, noteId);
        if (exist != null) {
            throw new BusinessException(ResultCode.ALREADY_COLLECTED);
        }
        Collection collection = new Collection();
        collection.setUserId(userId);
        collection.setNoteId(noteId);
        collectionMapper.insert(collection);
        // 同步更新笔记收藏数
        try {
            contentClient.incrementCollectCount(noteId);
        } catch (Exception e) {
            log.warn("同步收藏计数失败: noteId={}, error={}", noteId, e.getMessage());
        }
        // 发送收藏通知
        sendCollectNotification(noteId, userId);
        log.info("收藏成功: userId={}, noteId={}", userId, noteId);
    }

    private void sendCollectNotification(Long noteId, Long fromUserId) {
        try {
            var noteResult = contentClient.getNoteById(noteId);
            if (noteResult != null && noteResult.getData() != null) {
                Long targetUserId = Long.valueOf(noteResult.getData().get("userId").toString());
                if (targetUserId.equals(fromUserId)) {
                    return;
                }
                Map<String, Object> notification = new HashMap<>();
                notification.put("userId", targetUserId);
                notification.put("type", "COLLECT");
                notification.put("sourceId", noteId);
                notification.put("sourceType", "note");
                notification.put("content", "收藏了你的笔记");
                notification.put("fromUserId", fromUserId);
                notificationClient.createNotification(notification);
            }
        } catch (Exception e) {
            log.warn("发送收藏通知失败: noteId={}, error={}", noteId, e.getMessage());
        }
    }

    @Override
    public void uncollect(Long userId, Long noteId) {
        Collection exist = collectionMapper.selectByUserAndNote(userId, noteId);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_COLLECTED);
        }
        collectionMapper.deleteById(exist.getId());
        // 同步更新笔记收藏数
        try {
            contentClient.decrementCollectCount(noteId);
        } catch (Exception e) {
            log.warn("同步取消收藏计数失败: noteId={}, error={}", noteId, e.getMessage());
        }
        log.info("取消收藏成功: userId={}, noteId={}", userId, noteId);
    }

    @Override
    public boolean hasCollected(Long userId, Long noteId) {
        return collectionMapper.selectByUserAndNote(userId, noteId) != null;
    }

    @Override
    public Page<Collection> getMyCollections(Long userId, long current, long size) {
        Page<Collection> page = new Page<>(current, size);
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId)
               .orderByDesc(Collection::getCreateTime);
        return collectionMapper.selectPage(page, wrapper);
    }
}
