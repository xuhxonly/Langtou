package com.langtou.interact.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.interact.entity.Collection;

public interface CollectionService {

    void collect(Long userId, Long noteId);

    void uncollect(Long userId, Long noteId);

    boolean hasCollected(Long userId, Long noteId);

    Page<Collection> getMyCollections(Long userId, long current, long size);
}
