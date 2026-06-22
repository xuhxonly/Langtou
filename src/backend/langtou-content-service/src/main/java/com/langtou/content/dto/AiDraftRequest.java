package com.langtou.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * AI文案草稿生成请求
 */
@Data
public class AiDraftRequest {

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    private String title;

    /**
     * 文案风格: 种草/测评/教程/Vlog
     */
    @NotBlank(message = "文案风格不能为空")
    @Pattern(regexp = "种草|测评|教程|Vlog", message = "文案风格必须是: 种草、测评、教程、Vlog 之一")
    private String style;

    /**
     * 关键词列表（可选）
     */
    private List<String> keywords;
}
