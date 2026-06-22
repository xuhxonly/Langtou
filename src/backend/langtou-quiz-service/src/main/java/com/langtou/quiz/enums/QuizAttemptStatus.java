package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum QuizAttemptStatus {

    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    ABANDONED("ABANDONED");

    @EnumValue
    private final String value;

    QuizAttemptStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
