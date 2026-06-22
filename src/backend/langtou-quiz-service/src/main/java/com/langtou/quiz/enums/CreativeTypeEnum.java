package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum CreativeTypeEnum {

    SITUATIONAL("SITUATIONAL"),
    VARIANT("VARIANT"),
    STORY("STORY"),
    GAME("GAME"),
    PUZZLE("PUZZLE");

    @EnumValue
    private final String value;

    CreativeTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CreativeTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (CreativeTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
