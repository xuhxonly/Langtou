package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI封面推荐响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCoverRecommendResponse {

    /**
     * 封面建议列表（按评分降序）
     */
    private List<AiCoverSuggestion> recommendations;
}
