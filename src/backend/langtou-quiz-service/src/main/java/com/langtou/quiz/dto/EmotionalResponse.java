package com.langtou.quiz.dto;

import com.langtou.quiz.enums.EmotionEnum;
import lombok.Data;

@Data
public class EmotionalResponse {

    private EmotionEnum emotion;

    private String text;

    private String tone;

    private String actionSuggestion;

    private String companionReply;
}
