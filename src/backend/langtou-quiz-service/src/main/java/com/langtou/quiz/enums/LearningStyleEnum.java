package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum LearningStyleEnum {

    VISUAL("visual", "视觉型"),
    LOGICAL("logical", "逻辑型"),
    CREATIVE("creative", "创造型"),
    PERSISTENT("persistent", "坚持型");

    @EnumValue
    private final String value;

    private final String label;

    LearningStyleEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static LearningStyleEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (LearningStyleEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
