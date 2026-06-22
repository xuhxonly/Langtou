package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ConnectionTypeEnum {

    EXTENSION("EXTENSION"),
    CHALLENGE("CHALLENGE"),
    CROSS_DOMAIN("CROSS_DOMAIN"),
    APPLICATION("APPLICATION"),
    HISTORY("HISTORY");

    @EnumValue
    private final String value;

    ConnectionTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConnectionTypeEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ConnectionTypeEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}
