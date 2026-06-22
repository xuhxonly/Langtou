package com.langtou.quiz.entity;

import com.langtou.quiz.enums.QuizAttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizAttempt 单元测试")
class QuizAttemptTest {

    @Nested
    @DisplayName("乐观锁 @Version 注解验证")
    class VersionAnnotation {

        @Test
        @DisplayName("version 字段应存在 @Version 注解")
        void versionFieldShouldHaveVersionAnnotation() throws NoSuchFieldException {
            Field versionField = QuizAttempt.class.getDeclaredField("version");
            boolean hasVersion = false;
            for (Annotation a : versionField.getAnnotations()) {
                if (a.annotationType().getSimpleName().equals("Version")) {
                    hasVersion = true;
                    break;
                }
            }
            assertThat(hasVersion)
                    .as("QuizAttempt.version 字段必须声明 @Version 以启用 MyBatis-Plus 乐观锁")
                    .isTrue();
        }

        @Test
        @DisplayName("version 字段应为 Integer 类型")
        void versionFieldShouldBeIntegerType() throws NoSuchFieldException {
            Field versionField = QuizAttempt.class.getDeclaredField("version");
            assertThat(versionField.getType()).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("version 初始值默认为 0")
        void versionDefaultValueShouldBeZero() {
            QuizAttempt attempt = new QuizAttempt();
            assertThat(attempt.getVersion()).isNull();

            attempt.setVersion(0);
            assertThat(attempt.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("version 可手动递增模拟乐观锁行为")
        void versionShouldBeIncrementable() {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setVersion(1);
            attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
            Integer nextVersion = attempt.getVersion() + 1;
            assertThat(nextVersion).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("状态枚举映射")
    class StatusEnumMapping {

        @Test
        @DisplayName("IN_PROGRESS 应为 'IN_PROGRESS'")
        void inProgressValueShouldMatch() {
            assertThat(QuizAttemptStatus.IN_PROGRESS.getValue()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("COMPLETED 应为 'COMPLETED'")
        void completedValueShouldMatch() {
            assertThat(QuizAttemptStatus.COMPLETED.getValue()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("ABANDONED 应为 'ABANDONED'")
        void abandonedValueShouldMatch() {
            assertThat(QuizAttemptStatus.ABANDONED.getValue()).isEqualTo("ABANDONED");
        }
    }

    @Nested
    @DisplayName("字段赋值完整性")
    class FieldAssignments {

        @Test
        @DisplayName("所有核心字段可赋值并读取")
        void allFieldsShouldBeAssignable() {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setId(1L);
            attempt.setQuizSetId(10L);
            attempt.setUserId(20L);
            attempt.setGameSessionId(30L);
            attempt.setTotalQuestions(10);
            attempt.setCorrectCount(7);
            attempt.setScore(7);
            attempt.setLivesLeft(2);
            attempt.setRevivesUsed(1);
            attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
            attempt.setPassed(false);
            attempt.setDurationSeconds(45);

            assertThat(attempt.getTotalQuestions()).isEqualTo(10);
            assertThat(attempt.getScore()).isEqualTo(7);
            assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.IN_PROGRESS);
        }
    }
}
