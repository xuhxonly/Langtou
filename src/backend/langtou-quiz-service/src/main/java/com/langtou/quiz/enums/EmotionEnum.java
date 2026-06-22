﻿package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum EmotionEnum {

    FRUSTRATION("frustration", "frustration", "frustration"),
    FLOW("flow", "flow", "flow"),
    BOREDOM("boredom", "boredom", "boredom"),
    EXCITEMENT("excitement", "excitement", "excitement"),
    CALM("calm", "calm", "calm");

    @EnumValue
    private final String value;

    private final String label;

    private final String emoji;

    EmotionEnum(String value, String label, String emoji) {
        this.value = value;
        this.label = label;
        this.emoji = emoji;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public static EmotionEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (EmotionEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
