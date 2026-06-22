package com.langtou.quiz.controller;

import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.quiz.dto.QuizGenerateRequest;
import com.langtou.quiz.dto.QuizGenerateResponse;
import com.langtou.quiz.dto.QuizResultResponse;
import com.langtou.quiz.dto.QuizStartRequest;
import com.langtou.quiz.dto.QuizSubmitRequest;
import com.langtou.quiz.entity.QuizAttempt;
import com.langtou.quiz.entity.QuizSet;
import com.langtou.quiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
@Validated
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/generate")
    public Result<QuizGenerateResponse> generateQuiz(
            @Valid @RequestBody QuizGenerateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(quizService.generateQuizFromNote(request, userId));
    }

    @GetMapping("/sets/{id}")
    public Result<QuizSet> getQuizSet(@PathVariable Long id) {
        return Result.success(quizService.getQuizSet(id));
    }

    @PostMapping("/attempts")
    public Result<QuizAttempt> startAttempt(
            @Valid @RequestBody QuizStartRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(quizService.startAttempt(request, userId));
    }

    @PostMapping("/attempts/{id}/submit")
    public Result<QuizResultResponse> submitAnswers(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        request.setAttemptId(id);
        return Result.success(quizService.submitAnswers(request, userId));
    }

    @GetMapping("/attempts/{id}")
    public Result<QuizAttempt> getAttempt(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(quizService.getAttempt(id, userId));
    }

    @GetMapping("/sets/my")
    public Result<PageResult<QuizSet>> getMyQuizSets(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(quizService.getMyQuizSets(userId, page, size));
    }

    @GetMapping("/attempts/my")
    public Result<PageResult<QuizAttempt>> getMyAttempts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(quizService.getMyAttempts(userId, page, size));
    }
}
