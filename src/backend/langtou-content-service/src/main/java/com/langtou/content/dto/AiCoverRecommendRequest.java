package com.langtou.content.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * AI封面推荐请求
 */
@Data
public class AiCoverRecommendRequest {

    /**
     * 图片URL列表（至少2张）
     */
    @NotEmpty(message = "图片URL列表不能为空")
    private List<String> imageUrls;
}
