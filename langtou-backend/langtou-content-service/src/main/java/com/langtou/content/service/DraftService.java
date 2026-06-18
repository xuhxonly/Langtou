package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.Draft;

public interface DraftService {

    Draft saveDraft(Long userId, Draft draft);

    Draft updateDraft(Long userId, Long draftId, Draft draft);

    PageResult<Draft> getDraftList(Long userId, int page, int size);

    Draft getDraftDetail(Long userId, Long draftId);

    void deleteDraft(Long userId, Long draftId);
}
