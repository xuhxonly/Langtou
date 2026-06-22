package com.langtou.quiz.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizSubmitRequest 参数校验测试")
class QuizSubmitRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private QuizSubmitRequest buildValidRequest() {
        QuizSubmitRequest req = new QuizSubmitRequest();
        req.setAttemptId(1L);
        QuizSubmitRequest.AnswerItem item = new QuizSubmitRequest.AnswerItem();
        item.setSequenceNo(1);
        item.setSelected("A");
        req.setAnswers(Arrays.asList(item));
        return req;
    }

    @Nested
    @DisplayName("attemptId 校验")
    class AttemptIdValidation {

        @Test
        @DisplayName("attemptId 为 null 时应拒绝")
        void shouldRejectWhenAttemptIdIsNull() {
            QuizSubmitRequest req = buildValidRequest();
            req.setAttemptId(null);

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getPropertyPath().toString())
                    .isEqualTo("attemptId");
        }
    }

    @Nested
    @DisplayName("answers 列表结构校验")
    class AnswersValidation {

        @Test
        @DisplayName("answers 为 null 时应拒绝")
        void shouldRejectWhenAnswersIsNull() {
            QuizSubmitRequest req = buildValidRequest();
            req.setAnswers(null);

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
            assertThat(violations.iterator().next().getPropertyPath().toString())
                    .isEqualTo("answers");
        }

        @Test
        @DisplayName("answers 为空列表时应拒绝")
        void shouldRejectWhenAnswersIsEmpty() {
            QuizSubmitRequest req = buildValidRequest();
            req.setAnswers(new ArrayList<>());

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations)
                    .as("answers 为空列表应触发 @NotEmpty")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("answers 列表中元素 sequenceNo 为 null 时应拒绝")
        void shouldRejectWhenItemSequenceNoIsNull() {
            QuizSubmitRequest req = buildValidRequest();
            QuizSubmitRequest.AnswerItem item = new QuizSubmitRequest.AnswerItem();
            item.setSequenceNo(null);
            item.setSelected("A");
            req.setAnswers(Arrays.asList(item));

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("answers 列表中元素 selected 为 null 时应拒绝")
        void shouldRejectWhenItemSelectedIsNull() {
            QuizSubmitRequest req = buildValidRequest();
            QuizSubmitRequest.AnswerItem item = new QuizSubmitRequest.AnswerItem();
            item.setSequenceNo(1);
            item.setSelected(null);
            req.setAnswers(Arrays.asList(item));

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("sequenceNo 范围校验（超出题目数量的防御性校验，仅演示逻辑）")
    class SequenceNoRangeValidation {

        @Test
        @DisplayName("sequenceNo 为负数时，基础 @NotNull 仍通过但业务层应二次校验")
        void negativeSequenceNoShouldPassBeanValidation() {
            QuizSubmitRequest req = buildValidRequest();
            QuizSubmitRequest.AnswerItem item = new QuizSubmitRequest.AnswerItem();
            item.setSequenceNo(-1);
            item.setSelected("A");
            req.setAnswers(Arrays.asList(item));

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations)
                    .as("Bean Validation 层面未限制负数，业务层需二次校验")
                    .isEmpty();
        }

        @Test
        @DisplayName("sequenceNo 超过题目数量（如 999）时 Bean Validation 仍通过，业务层应拦截")
        void outOfRangeSequenceNoShouldPassBeanValidation() {
            QuizSubmitRequest req = buildValidRequest();
            QuizSubmitRequest.AnswerItem item = new QuizSubmitRequest.AnswerItem();
            item.setSequenceNo(999);
            item.setSelected("A");
            req.setAnswers(Arrays.asList(item));

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("完整合法请求")
    class ValidRequest {

        @Test
        @DisplayName("合法请求不应有任何校验违规")
        void shouldPassWhenAllFieldsValid() {
            QuizSubmitRequest req = buildValidRequest();

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("多答案列表合法时应通过校验")
        void shouldPassWithMultipleAnswers() {
            QuizSubmitRequest req = new QuizSubmitRequest();
            req.setAttemptId(100L);

            QuizSubmitRequest.AnswerItem a1 = new QuizSubmitRequest.AnswerItem();
            a1.setSequenceNo(1);
            a1.setSelected("A");
            QuizSubmitRequest.AnswerItem a2 = new QuizSubmitRequest.AnswerItem();
            a2.setSequenceNo(2);
            a2.setSelected("B");
            QuizSubmitRequest.AnswerItem a3 = new QuizSubmitRequest.AnswerItem();
            a3.setSequenceNo(3);
            a3.setSelected("[\"A\",\"C\"]");

            List<QuizSubmitRequest.AnswerItem> answers = Arrays.asList(a1, a2, a3);
            req.setAnswers(answers);
            req.setDurationSeconds(120);

            Set<ConstraintViolation<QuizSubmitRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }
}
