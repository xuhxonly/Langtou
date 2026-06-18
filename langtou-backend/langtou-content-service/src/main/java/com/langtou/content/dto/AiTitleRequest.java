package com.langtou.content.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI标题生成请求
 */
@Data
public class AiTitleRequest {

    /**
     * 图片URL列表
     */
    @NotEmpty(message = "图片URL列表不能为空")
    private List<String> imageUrls;

    /**
     * 内容类型（可选）
     */
    private String contentType;
}
