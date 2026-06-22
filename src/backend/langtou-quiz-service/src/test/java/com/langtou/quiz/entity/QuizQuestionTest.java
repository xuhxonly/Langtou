package com.langtou.quiz.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.quiz.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizQuestion 单元测试")
class QuizQuestionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("题型枚举映射（@EnumValue）")
    class QuestionTypeEnumMapping {

        @Test
        @DisplayName("SINGLE 枚举值应为 'SINGLE'")
        void singleEnumValueShouldBeSingle() {
            assertThat(QuestionType.SINGLE.getValue()).isEqualTo("SINGLE");
        }

        @Test
        @DisplayName("MULTI 枚举值应为 'MULTI'")
        void multiEnumValueShouldBeMulti() {
            assertThat(QuestionType.MULTI.getValue()).isEqualTo("MULTI");
        }

        @Test
        @DisplayName("JUDGE 枚举值应为 'JUDGE'")
        void judgeEnumValueShouldBeJudge() {
            assertThat(QuestionType.JUDGE.getValue()).isEqualTo("JUDGE");
        }

        @Test
        @DisplayName("QuestionType.values() 应包含 3 种题型")
        void shouldHaveThreeQuestionTypes() {
            assertThat(QuestionType.values()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("多选 correctAnswer 序列化")
    class MultiAnswerSerialization {

        @Test
        @DisplayName("单选 correctAnswer 为简单字符串")
        void singleAnswerShouldBePlainString() {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionType(QuestionType.SINGLE);
            q.setCorrectAnswer("C");
            assertThat(q.getCorrectAnswer()).isEqualTo("C");
        }

        @Test
        @DisplayName("多选 correctAnswer 以 JSON 数组字符串形式存储")
        void multiAnswerShouldBeJsonArrayString() {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionType(QuestionType.MULTI);
            String multiAnswerJson = "[\"A\",\"B\",\"D\"]";
            q.setCorrectAnswer(multiAnswerJson);

            assertThat(q.getCorrectAnswer()).isEqualTo("[\"A\",\"B\",\"D\"]");
        }

        @Test
        @DisplayName("多选 correctAnswer 字符串可反序列化为选项列表")
        void multiAnswerStringShouldBeDeserializable() throws JsonProcessingException {
            String multiAnswerJson = "[\"A\",\"C\"]";
            java.util.List<String> answers = objectMapper.readValue(
                    multiAnswerJson,
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)
            );
            assertThat(answers).containsExactly("A", "C");
        }

        @Test
        @DisplayName("判断题 correctAnswer 为 'A'(正确) 或 'B'(错误)")
        void judgeAnswerShouldBeAorB() {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionType(QuestionType.JUDGE);
            q.setCorrectAnswer("A");
            assertThat(q.getCorrectAnswer()).isEqualTo("A");
        }

        @Test
        @DisplayName("QuizQuestion 类 questionType 字段枚举正确赋值")
        void questionTypeFieldShouldBeAssignable() {
            QuizQuestion q = new QuizQuestion();
            q.setQuestionType(QuestionType.MULTI);
            q.setStem("以下哪些是正确的？");
            q.setOptionA("A");
            q.setOptionB("B");
            q.setOptionC("C");
            q.setOptionD("D");
            q.setCorrectAnswer("[\"A\",\"C\"]");

            assertThat(q.getQuestionType()).isEqualTo(QuestionType.MULTI);
            assertThat(q.getStem()).isEqualTo("以下哪些是正确的？");
        }
    }
}
