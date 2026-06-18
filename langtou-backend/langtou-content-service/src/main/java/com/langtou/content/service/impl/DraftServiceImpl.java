package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.ResultCode;
import com.langtou.content.entity.Draft;
import com.langtou.content.mapper.DraftMapper;
import com.langtou.content.service.DraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftServiceImpl implements DraftService {

    private final DraftMapper draftMapper;

    @Override
    public Draft saveDraft(Long userId, Draft draft) {
        draft.setUserId(userId);
        draft.setStatus(0);
        draftMapper.insert(draft);
        log.info("保存草稿成功: userId={}, draftId={}", userId, draft.getId());
        return draft;
    }

    @Override
    public Draft updateDraft(Long userId, Long draftId, Draft draft) {
        Draft existing = draftMapper.selectById(draftId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.DRAFT_NOT_FOUND);
        }
        draft.setId(draftId);
        draft.setUserId(userId);
        draftMapper.updateById(draft);
        log.info("更新草稿成功: userId={}, draftId={}", userId, draftId);
        return draftMapper.selectById(draftId);
    }

    @Override
    public PageResult<Draft> getDraftList(Long userId, int page, int size) {
        Page<Draft> pageParam = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Draft> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Draft::getUserId, userId).orderByDesc(Draft::getUpdatedAt);
        Page<Draft> result = draftMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Draft getDraftDetail(Long userId, Long draftId) {
        Draft draft = draftMapper.selectById(draftId);
        if (draft == null || !draft.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.DRAFT_NOT_FOUND);
        }
        return draft;
    }

    @Override
    public void deleteDraft(Long userId, Long draftId) {
        Draft draft = draftMapper.selectById(draftId);
        if (draft == null || !draft.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.DRAFT_NOT_FOUND);
        }
        draftMapper.deleteById(draftId);
        log.info("删除草稿成功: userId={}, draftId={}", userId, draftId);
    }
}
