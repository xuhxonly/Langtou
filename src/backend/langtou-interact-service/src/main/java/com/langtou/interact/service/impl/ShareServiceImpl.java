package com.langtou.interact.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.entity.ShareRecord;
import com.langtou.interact.mapper.ShareRecordMapper;
import com.langtou.interact.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareRecordMapper shareRecordMapper;

    private static final Set<String> VALID_SHARE_TYPES = Set.of("link", "image", "wechat");

    @Override
    public ShareRecord share(Long userId, Long noteId, String shareType) {
        if (shareType == null || !VALID_SHARE_TYPES.contains(shareType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "转发类型不合法，支持: link, image, wechat");
        }
        ShareRecord record = new ShareRecord();
        record.setUserId(userId);
        record.setNoteId(noteId);
        record.setShareType(shareType);
        shareRecordMapper.insert(record);
        log.info("转发成功: userId={}, noteId={}, shareType={}", userId, noteId, shareType);
        return record;
    }
}
