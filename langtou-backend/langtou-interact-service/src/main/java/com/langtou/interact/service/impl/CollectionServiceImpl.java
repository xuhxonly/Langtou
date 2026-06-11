package com.langtou.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.entity.Collection;
import com.langtou.interact.mapper.CollectionMapper;
import com.langtou.interact.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final com.langtou.common.client.ContentClient contentClient;

    @Override
    public void collect(Long userId, Long noteId) {
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
        log.info("收藏成功: userId={}, noteId={}", userId, noteId);
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
