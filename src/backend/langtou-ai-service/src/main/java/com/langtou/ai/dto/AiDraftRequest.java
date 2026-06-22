package com.langtou.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class AiDraftRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "文案风格不能为空")
    @Pattern(regexp = "种草|测评|教程|Vlog", message = "文案风格必须是: 种草、测评、教程、Vlog 之一")
    private String style;

    private List<String> keywords;
}