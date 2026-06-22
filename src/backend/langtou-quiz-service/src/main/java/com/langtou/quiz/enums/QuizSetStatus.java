package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum QuizSetStatus {

    PENDING("PENDING"),
    READY("READY"),
    FAILED("FAILED"),
    EXPIRED("EXPIRED"),
    PUBLISHED("PUBLISHED");

    @EnumValue
    private final String value;

    QuizSetStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
