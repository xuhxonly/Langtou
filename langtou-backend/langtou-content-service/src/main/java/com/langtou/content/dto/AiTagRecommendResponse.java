package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI标签推荐响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTagRecommendResponse {

    /**
     * 标签建议列表
     */
    private List<AiTagSuggestion> tags;
}
