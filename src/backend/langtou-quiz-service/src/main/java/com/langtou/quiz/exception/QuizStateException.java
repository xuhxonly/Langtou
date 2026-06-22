package com.langtou.quiz.exception;

import com.langtou.quiz.enums.QuizAttemptStatus;

public class QuizStateException extends QuizException {

    public QuizStateException(String message) {
        super(message);
    }

    public QuizStateException(Long attemptId, QuizAttemptStatus current, QuizAttemptStatus expected) {
        super("答题状态非法流转: attemptId=" + attemptId + ", current=" + current + ", expected=" + expected);
    }
}
