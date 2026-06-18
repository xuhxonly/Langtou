package com.langtou.content.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI标题生成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTitleResponse {

    /**
     * 标题建议列表
     */
    private List<AiTitleSuggestion> titles;
}
