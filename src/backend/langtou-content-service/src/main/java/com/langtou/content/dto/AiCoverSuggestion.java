package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI封面建议
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCoverSuggestion {

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 综合评分 (0-100)
     */
    private Double score;

    /**
     * 构图评分
     */
    private Double compositionScore;

    /**
     * 色彩评分
     */
    private Double colorScore;

    /**
     * 清晰度评分
     */
    private Double clarityScore;

    /**
     * 裁剪后的图片URL（3:4比例）
     */
    private String cropUrl;
}
