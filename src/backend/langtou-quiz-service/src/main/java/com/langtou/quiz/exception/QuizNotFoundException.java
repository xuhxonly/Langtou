package com.langtou.quiz.exception;

public class QuizNotFoundException extends QuizException {

    public QuizNotFoundException(String message) {
        super(message);
    }

    public QuizNotFoundException(Long quizSetId) {
        super("关卡不存在: " + quizSetId);
    }
}
