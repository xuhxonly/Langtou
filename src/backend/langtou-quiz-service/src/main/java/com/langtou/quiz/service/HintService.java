package com.langtou.quiz.service;

import com.langtou.quiz.dto.HintResponse;
import com.langtou.quiz.dto.HintResult;
import com.langtou.quiz.entity.QuizQuestion;

public interface HintService {

    HintResponse requestHint(Long userId, Long quizSetId, Long questionId, int currentHintLevel);

    HintResult getHintByLevel(int level, QuizQuestion question);

    void recordHintRequest(Long userId, Long quizSetId, Long questionId, int level, String hintContent);
}
