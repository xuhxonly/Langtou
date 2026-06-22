package com.langtou.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiDraftResponse {

    private String draft;

    private Integer wordCount;
}