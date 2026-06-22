package com.langtou.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCoverSuggestion {

    private String imageUrl;

    private Double score;

    private Double compositionScore;

    private Double colorScore;

    private Double clarityScore;

    private String cropUrl;
}