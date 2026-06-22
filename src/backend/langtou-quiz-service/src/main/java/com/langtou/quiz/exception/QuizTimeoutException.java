package com.langtou.quiz.exception;

public class QuizTimeoutException extends QuizException {

    public QuizTimeoutException(String message) {
        super(message);
    }

    public QuizTimeoutException(Long attemptId, int usedSeconds, int limitSeconds) {
        super("答题超时: attemptId=" + attemptId + ", used=" + usedSeconds + "s, limit=" + limitSeconds + "s");
    }
}
