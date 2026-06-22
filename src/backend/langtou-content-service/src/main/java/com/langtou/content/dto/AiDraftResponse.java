package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI文案草稿生成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDraftResponse {

    /**
     * 文案草稿内容
     */
    private String draft;

    /**
     * 字数
     */
    private Integer wordCount;
}
