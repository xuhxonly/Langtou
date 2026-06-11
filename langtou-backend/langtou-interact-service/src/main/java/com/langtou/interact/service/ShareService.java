package com.langtou.interact.service;

import com.langtou.interact.entity.ShareRecord;

public interface ShareService {

    ShareRecord share(Long userId, Long noteId, String shareType);
}
