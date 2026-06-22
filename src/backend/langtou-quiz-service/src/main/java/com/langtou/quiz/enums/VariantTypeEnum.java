package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum VariantTypeEnum {

    ROTATE("ROTATE"),
    EXPAND("EXPAND"),
    NARROW("NARROW"),
    CONTEXT_SWAP("CONTEXT_SWAP"),
    MULTI_STEP("MULTI_STEP");

    @EnumValue
    private final String value;

    VariantTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static VariantTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (VariantTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
