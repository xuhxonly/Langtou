package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TrendEnum {

    UP("up", "上升"),
    DOWN("down", "下降"),
    STABLE("stable", "稳定");

    @EnumValue
    private final String value;

    private final String label;

    TrendEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
