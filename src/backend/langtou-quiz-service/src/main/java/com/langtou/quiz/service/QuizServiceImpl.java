﻿﻿﻿package com.langtou.quiz.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.quiz.cache.QuizCacheManager;
import com.langtou.quiz.config.QuizProperties;
import com.langtou.quiz.dto.QuizGenerateRequest;
import com.langtou.quiz.dto.QuizGenerateResponse;
import com.langtou.quiz.dto.QuizGenerateResponse.QuestionItem;
import com.langtou.quiz.dto.QuizResultResponse;
import com.langtou.quiz.dto.QuizStartRequest;
import com.langtou.quiz.dto.QuizSubmitRequest;
import com.langtou.quiz.dto.QuizSubmitRequest.AnswerItem;
import com.langtou.quiz.entity.QuizAttempt;
import com.langtou.quiz.entity.QuizQuestion;
import com.langtou.quiz.entity.QuizSet;
import com.langtou.quiz.enums.QuizAttemptStatus;
import com.langtou.quiz.enums.QuizSetStatus;
import com.langtou.quiz.exception.QuizException;
import com.langtou.quiz.exception.QuizNotFoundException;
import com.langtou.quiz.exception.QuizStateException;
import com.langtou.quiz.exception.QuizTimeoutException;
import com.langtou.quiz.feign.AiServiceClient;
import com.langtou.quiz.feign.dto.AiGenerateRequest;
import com.langtou.quiz.feign.dto.AiGenerateResponse;
import com.langtou.quiz.mapper.QuizAttemptMapper;
import com.langtou.quiz.mapper.QuizQuestionMapper;
import com.langtou.quiz.mapper.QuizSetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final AiServiceClient aiServiceClient;
    private final QuizSetMapper quizSetMapper;
    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizAttemptMapper quizAttemptMapper;
    private final QuizProperties quizProperties;
    private final QuizCacheManager quizCacheManager;

    @Override
    @Transactional
    public QuizGenerateResponse generateQuizFromNote(QuizGenerateRequest request, Long creatorId) {
        log.info("开始为笔记生成关卡: noteId={}, creatorId={}", request.getNoteId(), creatorId);

        AiGenerateRequest aiRequest = new AiGenerateRequest();
        aiRequest.setNoteId(request.getNoteId());
        aiRequest.setQuestionCount(request.getQuestionCount());
        aiRequest.setDifficulty("NORMAL");

        Result<AiGenerateResponse> aiResult = aiServiceClient.generateQuiz(aiRequest);
        if (aiResult == null || !aiResult.getCode().equals(200) || aiResult.getData() == null) {
            log.error("AI 生成题目失败: noteId={}, message={}", request.getNoteId(),
                    aiResult == null ? "null" : aiResult.getMessage());
            throw new QuizException("AI 生成题目失败: " +
                    (aiResult == null ? "服务无响应" : aiResult.getMessage()));
        }

        AiGenerateResponse data = aiResult.getData();
        List<AiGenerateResponse.QuestionItem> aiQuestions = data.getQuestions();
        if (aiQuestions == null || aiQuestions.isEmpty()) {
            throw new QuizException("AI 未生成题目: noteId=" + request.getNoteId());
        }

        QuizSet quizSet = new QuizSet();
        quizSet.setNoteId(request.getNoteId());
        quizSet.setCreatorId(creatorId);
        quizSet.setTitle("笔记闯关-" + request.getNoteId());
        quizSet.setQuestionCount(aiQuestions.size());
        quizSet.setStatus(QuizSetStatus.READY);
        quizSet.setSource("AI");
        quizSet.setPromptHash(data.getPromptHash());
        quizSet.setCorrectRate(0);
        quizSetMapper.insert(quizSet);

        for (AiGenerateResponse.QuestionItem q : aiQuestions) {
            QuizQuestion question = new QuizQuestion();
            question.setQuizSetId(quizSet.getId());
            question.setSequenceNo(q.getSequenceNo());
            question.setStem(q.getStem());
            question.setOptionA(q.getOptionA());
            question.setOptionB(q.getOptionB());
            question.setOptionC(q.getOptionC());
            question.setOptionD(q.getOptionD());
            question.setCorrectAnswer(q.getCorrectAnswer());
            question.setExplanation(q.getExplanation());
            question.setScore(q.getScore() == null ? 1 : q.getScore());
            quizQuestionMapper.insert(question);
        }

        QuizGenerateResponse response = new QuizGenerateResponse();
        response.setQuizSetId(quizSet.getId());
        response.setNoteId(quizSet.getNoteId());
        response.setQuestionCount(quizSet.getQuestionCount());
        response.setStatus(quizSet.getStatus().getValue());

        Map<Integer, QuizQuestion> qMap = new HashMap<>();
        List<QuizQuestion> savedQuestions = quizQuestionMapper.listByQuizSetId(quizSet.getId());
        for (QuizQuestion q : savedQuestions) {
            QuestionItem item = new QuestionItem();
            item.setSequenceNo(q.getSequenceNo());
            item.setStem(q.getStem());
            item.setOptionA(q.getOptionA());
            item.setOptionB(q.getOptionB());
            item.setOptionC(q.getOptionC());
            item.setOptionD(q.getOptionD());
            item.setCorrectAnswer(q.getCorrectAnswer());
            item.setExplanation(q.getExplanation());
            qMap.put(q.getSequenceNo(), q);
        }

        response.setQuestions(new java.util.ArrayList<>());
        for (QuizQuestion q : savedQuestions) {
            QuestionItem item = new QuestionItem();
            item.setSequenceNo(q.getSequenceNo());
            item.setStem(q.getStem());
            item.setOptionA(q.getOptionA());
            item.setOptionB(q.getOptionB());
            item.setOptionC(q.getOptionC());
            item.setOptionD(q.getOptionD());
            item.setCorrectAnswer(q.getCorrectAnswer());
            item.setExplanation(q.getExplanation());
            response.getQuestions().add(item);
        }

        log.info("关卡生成完成: quizSetId={}, questionCount={}", quizSet.getId(), quizSet.getQuestionCount());
        return response;
    }

    @Override
    public QuizSet getQuizSet(Long quizSetId) {
        log.info("获取关卡详情: quizSetId={}", quizSetId);
        QuizSet cached = quizCacheManager.getQuizSet(quizSetId);
        if (cached != null) {
            return cached;
        }
        QuizSet quizSet = quizSetMapper.selectById(quizSetId);
        if (quizSet == null) {
            throw new QuizNotFoundException(quizSetId);
        }
        quizCacheManager.cacheQuizSet(quizSet);
        return quizSet;
    }

    @Override
    @Transactional
    public QuizAttempt startAttempt(QuizStartRequest request, Long userId) {
        log.info("开始答题: quizSetId={}, userId={}", request.getQuizSetId(), userId);
        QuizSet quizSet = quizSetMapper.selectById(request.getQuizSetId());
        if (quizSet == null) {
            throw new QuizNotFoundException(request.getQuizSetId());
        }
        if (quizSet.getStatus() != QuizSetStatus.READY
                && quizSet.getStatus() != QuizSetStatus.PUBLISHED) {
            throw new QuizStateException("关卡不可用: quizSetId=" + quizSet.getId() + ", status=" + quizSet.getStatus());
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuizSetId(quizSet.getId());
        attempt.setUserId(userId);
        attempt.setGameSessionId(request.getGameSessionId());
        attempt.setTotalQuestions(quizSet.getQuestionCount());
        attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
        attempt.setLivesLeft(3);
        attempt.setRevivesUsed(0);
        attempt.setScore(0);
        attempt.setCorrectCount(0);
        attemptMapper.insert(attempt);

        log.info("答题开始成功: attemptId={}, quizSetId={}, userId={}", attempt.getId(), quizSet.getId(), userId);
        return attempt;
    }

    @Override
    @Transactional
    public QuizResultResponse submitAnswers(QuizSubmitRequest request, Long userId) {
        Long attemptId = request.getAttemptId();
        log.info("提交答案: attemptId={}, userId={}", attemptId, userId);

        boolean locked = quizCacheManager.trySubmitLock(attemptId);
        if (!locked) {
            log.warn("幂等锁命中，拒绝重复提交: attemptId={}", attemptId);
            throw new QuizStateException("请勿重复提交答题: attemptId=" + attemptId);
        }

        QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            quizCacheManager.releaseSubmitLock(attemptId);
            throw new QuizStateException("答题记录不存在: attemptId=" + attemptId);
        }

        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            quizCacheManager.releaseSubmitLock(attemptId);
            throw new QuizStateException(attemptId, QuizAttemptStatus.IN_PROGRESS, attempt.getStatus());
        }

        int totalSecondsLimit = quizProperties.getQuestion().getPerQuestionSeconds() * attempt.getTotalQuestions();
        Integer duration = request.getDurationSeconds();
        if (duration != null && duration > totalSecondsLimit) {
            quizCacheManager.releaseSubmitLock(attemptId);
            throw new QuizTimeoutException(attemptId, duration, totalSecondsLimit);
        }

        List<QuizQuestion> questions = quizQuestionMapper.listByQuizSetId(attempt.getQuizSetId());
        Map<Integer, QuizQuestion> qMap = new HashMap<>();
        for (QuizQuestion q : questions) {
            qMap.put(q.getSequenceNo(), q);
        }

        int correct = 0;
        int score = 0;
        for (AnswerItem item : request.getAnswers()) {
            QuizQuestion q = qMap.get(item.getSequenceNo());
            if (q == null) {
                quizCacheManager.releaseSubmitLock(attemptId);
                throw new QuizStateException("题目序号超出范围: sequenceNo=" + item.getSequenceNo());
            }
            if (q.getCorrectAnswer() != null && q.getCorrectAnswer().equals(item.getSelected())) {
                correct += 1;
                score += q.getScore() == null ? 1 : q.getScore();
            }
        }

        attempt.setCorrectCount(correct);
        attempt.setScore(score);
        attempt.setDurationSeconds(duration);
        attempt.setStatus(QuizAttemptStatus.COMPLETED);

        int passingScore = quizProperties.getQuestion().getPassingScore();
        boolean passed = score >= passingScore;
        attempt.setPassed(passed);

        try {
            int rows = quizAttemptMapper.updateById(attempt);
            if (rows == 0) {
                throw new QuizStateException("答题记录更新失败（乐观锁冲突）: attemptId=" + attemptId);
            }
        } catch (RuntimeException e) {
            quizCacheManager.releaseSubmitLock(attemptId);
            throw e;
        }

        QuizSet quizSet = quizSetMapper.selectById(attempt.getQuizSetId());
        if (quizSet != null) {
            int total = quizSet.getQuestionCount() == null ? questions.size() : quizSet.getQuestionCount();
            int correctRate = total == 0 ? 0 : (int) Math.round(correct * 100.0 / total);
            quizSet.setCorrectRate(correctRate);
            quizSetMapper.updateById(quizSet);
            quizCacheManager.evictQuizSet(quizSet.getId());
            quizCacheManager.cacheQuizSet(quizSet);
        }

        int stars = calculateStars(correct, attempt.getTotalQuestions());

        QuizResultResponse response = new QuizResultResponse();
        response.setAttemptId(attempt.getId());
        response.setQuizSetId(attempt.getQuizSetId());
        response.setTotalQuestions(attempt.getTotalQuestions());
        response.setCorrectCount(correct);
        response.setScore(score);
        response.setPassed(passed);
        response.setRank(stars);

        log.info("答题提交完成: attemptId={}, correct={}, score={}, passed={}", attemptId, correct, score, passed);
        return response;
    }

    @Override
    public QuizAttempt getAttempt(Long attemptId, Long userId) {
        log.info("获取答题记录: attemptId={}, userId={}", attemptId, userId);
        QuizAttempt attempt = quizAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new QuizStateException("答题记录不存在: attemptId=" + attemptId);
        }
        if (userId != null && !userId.equals(attempt.getUserId())) {
            throw new QuizStateException("无权访问该答题记录: attemptId=" + attemptId);
        }
        return attempt;
    }

    @Override
    public PageResult<QuizSet> getMyQuizSets(Long userId, int page, int size) {
        log.info("查询我的关卡列表: userId={}, page={}, size={}", userId, page, size);
        Page<QuizSet> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QuizSet> wrapper = new LambdaQueryWrapper<QuizSet>()
                .eq(QuizSet::getCreatorId, userId)
                .orderByDesc(QuizSet::getId);
        Page<QuizSet> result = quizSetMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public PageResult<QuizAttempt> getMyAttempts(Long userId, int page, int size) {
        log.info("查询我的答题历史: userId={}, page={}, size={}", userId, page, size);
        Page<QuizAttempt> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QuizAttempt> wrapper = new LambdaQueryWrapper<QuizAttempt>()
                .eq(QuizAttempt::getUserId, userId)
                .orderByDesc(QuizAttempt::getId);
        Page<QuizAttempt> result = quizAttemptMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private int calculateStars(int correct, int total) {
        if (total <= 0) {
            return 0;
        }
        double rate = (double) correct / total;
        if (rate >= 0.9) {
            return 3;
        }
        if (rate >= 0.7) {
            return 2;
        }
        if (rate >= 0.5) {
            return 1;
        }
        return 0;
    }
}



