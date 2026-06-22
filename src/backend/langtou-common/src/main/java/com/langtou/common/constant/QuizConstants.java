package com.langtou.common.constant;

public interface QuizConstants {

    int QUESTION_COUNT_MIN = 5;
    int QUESTION_COUNT_MAX = 12;
    int QUESTION_COUNT_DEFAULT = 10;

    int PER_QUESTION_SECONDS = 60;
    int LIFE_PER_QUESTION = 1;
    int PASSING_SCORE = 7;

    int MAX_REVIVE_PER_GAME = 2;
    int REVIVE_PRICE_FEN = 99;

    String STATUS_PENDING = "PENDING";
    String STATUS_READY = "READY";
    String STATUS_FAILED = "FAILED";
    String STATUS_EXPIRED = "EXPIRED";
    String STATUS_PUBLISHED = "PUBLISHED";

    String AUDIT_TYPE_QUIZ_APPROVAL = "quiz_approval";

    String DEGRADE_LEVEL_NONE = "NONE";
    String DEGRADE_LEVEL_L1 = "L1";
    String DEGRADE_LEVEL_L2 = "L2";
    String DEGRADE_LEVEL_L3 = "L3";

    String ATTR_QUIZ_ENABLED = "quiz_enabled";
    String ATTR_QUIZ_SET_ID = "quiz_set_id";
    String ATTR_QUIZ_STATUS = "quiz_status";
}
