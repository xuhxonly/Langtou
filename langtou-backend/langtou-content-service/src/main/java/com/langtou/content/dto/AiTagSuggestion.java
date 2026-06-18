package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI标签建议
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTagSuggestion {

    /**
     * 标签名
     */
    private String tagName;

    /**
     * 标签热度
     */
    private Long heat;

    /**
     * 标签分类 (core/scene/style/emotion)
     */
    private String category;
}
