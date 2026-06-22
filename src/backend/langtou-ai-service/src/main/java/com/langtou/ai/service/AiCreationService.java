package com.langtou.ai.service;

import com.langtou.ai.dto.AiCoverRecommendRequest;
import com.langtou.ai.dto.AiCoverRecommendResponse;
import com.langtou.ai.dto.AiDraftRequest;
import com.langtou.ai.dto.AiDraftResponse;
import com.langtou.ai.dto.AiTagRecommendRequest;
import com.langtou.ai.dto.AiTagRecommendResponse;
import com.langtou.ai.dto.AiTitleRequest;
import com.langtou.ai.dto.AiTitleResponse;

public interface AiCreationService {

    AiTitleResponse generateTitles(AiTitleRequest request);

    AiTagRecommendResponse recommendTags(AiTagRecommendRequest request);

    AiDraftResponse generateDraft(AiDraftRequest request);

    AiCoverRecommendResponse recommendCover(AiCoverRecommendRequest request);

    void checkRateLimit(Long userId);
}