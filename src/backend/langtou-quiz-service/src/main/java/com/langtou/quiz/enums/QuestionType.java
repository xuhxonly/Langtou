package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum QuestionType {

    SINGLE("SINGLE"),
    MULTI("MULTI"),
    JUDGE("JUDGE");

    @EnumValue
    private final String value;

    QuestionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
