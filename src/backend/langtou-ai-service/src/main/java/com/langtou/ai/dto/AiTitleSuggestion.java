package com.langtou.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTitleSuggestion {

    private String title;

    private String reason;

    private Double score;
}