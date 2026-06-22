package com.langtou.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTagSuggestion {

    private String tagName;

    private Long heat;

    private String category;
}