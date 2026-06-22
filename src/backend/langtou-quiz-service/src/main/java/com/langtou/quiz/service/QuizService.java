﻿﻿﻿package com.langtou.quiz.service;

import com.langtou.common.result.PageResult;
import com.langtou.quiz.dto.QuizGenerateRequest;
import com.langtou.quiz.dto.QuizGenerateResponse;
import com.langtou.quiz.dto.QuizResultResponse;
import com.langtou.quiz.dto.QuizStartRequest;
import com.langtou.quiz.dto.QuizSubmitRequest;
import com.langtou.quiz.entity.QuizAttempt;
import com.langtou.quiz.entity.QuizSet;

public interface QuizService {

    QuizGenerateResponse generateQuizFromNote(QuizGenerateRequest request, Long creatorId);

    QuizSet getQuizSet(Long quizSetId);

    QuizAttempt startAttempt(QuizStartRequest request, Long userId);

    QuizResultResponse submitAnswers(QuizSubmitRequest request, Long userId);

    QuizAttempt getAttempt(Long attemptId, Long userId);

    PageResult<QuizSet> getMyQuizSets(Long userId, int page, int size);

    PageResult<QuizAttempt> getMyAttempts(Long userId, int page, int size);
}
