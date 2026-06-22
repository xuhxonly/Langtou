package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI标题建议
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTitleSuggestion {

    /**
     * 标题内容
     */
    private String title;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 推荐分数 (0-100)
     */
    private Double score;
}
