package com.langtou.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AiTitleRequest {

    @NotEmpty(message = "图片URL列表不能为空")
    private List<String> imageUrls;

    private String contentType;
}