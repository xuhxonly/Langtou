package com.langtou.content.dto;

import lombok.Data;

import java.util.List;

/**
 * AI标签推荐请求
 */
@Data
public class AiTagRecommendRequest {

    /**
     * 图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 标题（可选）
     */
    private String title;

    /**
     * 正文内容（可选）
     */
    private String content;
}
