package com.langtou.quiz.config;

import com.langtou.common.result.Result;
import com.langtou.common.result.ResultCode;
import com.langtou.quiz.exception.QuizNotFoundException;
import com.langtou.quiz.exception.QuizStateException;
import com.langtou.quiz.exception.QuizTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class QuizExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNotFound(QuizNotFoundException e) {
        log.warn("关卡不存在: {}", e.getMessage());
        return Result.error(404, e.getMessage());
    }

    @ExceptionHandler(QuizStateException.class)
    public Result<?> handleStateError(QuizStateException e) {
        log.warn("关卡状态错误: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(QuizTimeoutException.class)
    public Result<?> handleTimeout(QuizTimeoutException e) {
        log.warn("答题超时: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), message);
    }
}
