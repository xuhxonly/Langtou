package com.langtou.quiz.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizGenerateRequest 参数校验测试")
class QuizGenerateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("@NotNull(noteId) 校验")
    class NoteIdValidation {

        @Test
        @DisplayName("noteId 为 null 时应抛出校验异常")
        void shouldRejectWhenNoteIdIsNull() {
            QuizGenerateRequest req = new QuizGenerateRequest();
            req.setNoteId(null);
            req.setQuestionCount(10);

            Set<ConstraintViolation<QuizGenerateRequest>> violations = validator.validate(req);

            assertThat(violations)
                    .as("noteId 为空时应产生校验违规")
                    .isNotEmpty();

            ConstraintViolation<QuizGenerateRequest> v = violations.iterator().next();
            assertThat(v.getPropertyPath().toString()).isEqualTo("noteId");
            assertThat(v.getMessage()).isEqualTo("笔记ID不能为空");
        }

        @Test
        @DisplayName("noteId 有值时不应产生校验异常")
        void shouldPassWhenNoteIdIsProvided() {
            QuizGenerateRequest req = new QuizGenerateRequest();
            req.setNoteId(1001L);
            req.setQuestionCount(10);

            Set<ConstraintViolation<QuizGenerateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("questionCount 为 null 时默认允许（无 @NotNull）")
        void shouldAllowNullQuestionCount() {
            QuizGenerateRequest req = new QuizGenerateRequest();
            req.setNoteId(1001L);
            req.setQuestionCount(null);

            Set<ConstraintViolation<QuizGenerateRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }
}
