package com.langtou.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiTagRecommendRequest {

    private List<String> imageUrls;

    private String title;

    private String content;
}