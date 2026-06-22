package com.langtou.quiz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SkillSubjectEnum {

    MATH("math", "数学"),
    HISTORY("history", "历史"),
    PROGRAMMING("programming", "编程"),
    CHINESE("chinese", "语文"),
    ENGLISH("english", "英语"),
    PHYSICS("physics", "物理"),
    CHEMISTRY("chemistry", "化学"),
    BIOLOGY("biology", "生物");

    @EnumValue
    private final String value;

    private final String label;

    SkillSubjectEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static SkillSubjectEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (SkillSubjectEnum e : values()) {
            if (e.value.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
