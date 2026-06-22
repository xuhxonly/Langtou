package com.langtou.content.service;

import com.langtou.content.dto.*;

/**
 * AI创作工具服务接口
 *
 * 提供AI辅助创作能力，包括标题生成、标签推荐、文案草稿和封面推荐。
 */
public interface AiCreationService {

    /**
     * AI生成标题
     *
     * @param request 标题生成请求（包含图片URL和内容类型）
     * @return 标题建议列表（3-5个候选）
     */
    AiTitleResponse generateTitles(AiTitleRequest request);

    /**
     * AI推荐标签
     *
     * @param request 标签推荐请求（包含图片URL、标题和内容）
     * @return 标签建议列表（5-10个）
     */
    AiTagRecommendResponse recommendTags(AiTagRecommendRequest request);

    /**
     * AI生成文案草稿
     *
     * @param request 文案草稿请求（包含标题、风格和关键词）
     * @return 文案草稿内容和字数
     */
    AiDraftResponse generateDraft(AiDraftRequest request);

    /**
     * AI推荐封面
     *
     * @param request 封面推荐请求（包含图片URL列表）
     * @return 封面建议列表（按评分降序排列）
     */
    AiCoverRecommendResponse recommendCover(AiCoverRecommendRequest request);
}
