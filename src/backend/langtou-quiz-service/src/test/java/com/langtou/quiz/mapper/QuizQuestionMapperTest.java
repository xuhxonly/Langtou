package com.langtou.quiz.mapper;

import com.langtou.quiz.entity.QuizQuestion;
import com.langtou.quiz.enums.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisPlusTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MybatisPlusTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:langtou_quiz_question_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql",
                "spring.sql.init.data-locations=classpath:data.sql",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.id-type=auto"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("QuizQuestionMapper 集成测试")
class QuizQuestionMapperTest {

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Nested
    @DisplayName("listByQuizSetId 排序正确性")
    class ListOrdering {

        @Test
        @DisplayName("按 sequence_no 升序返回题目列表")
        void shouldOrderBySequenceNoAsc() {
            List<QuizQuestion> questions = quizQuestionMapper.listByQuizSetId(1L);

            assertThat(questions).isNotEmpty();
            for (int i = 1; i < questions.size(); i++) {
                assertThat(questions.get(i).getSequenceNo())
                        .isGreaterThanOrEqualTo(questions.get(i - 1).getSequenceNo());
            }
        }

        @Test
        @DisplayName("不存在的 quizSetId 应返回空列表")
        void shouldReturnEmptyWhenSetNotExist() {
            List<QuizQuestion> questions = quizQuestionMapper.listByQuizSetId(9999L);
            assertThat(questions).isEmpty();
        }

        @Test
        @DisplayName("不同 quizSetId 间数据互不干扰")
        void shouldIsolateByQuizSet() {
            List<QuizQuestion> q1 = quizQuestionMapper.listByQuizSetId(1L);
            List<QuizQuestion> q2 = quizQuestionMapper.listByQuizSetId(2L);

            assertThat(q1).allSatisfy(q -> assertThat(q.getQuizSetId()).isEqualTo(1L));
            assertThat(q2).allSatisfy(q -> assertThat(q.getQuizSetId()).isEqualTo(2L));
        }
    }

    @Nested
    @DisplayName("CRUD 基础操作")
    class CrudOperations {

        @Test
        @DisplayName("insert 后 selectById 可读出")
        void shouldInsertAndSelect() {
            QuizQuestion q = new QuizQuestion();
            q.setQuizSetId(100L);
            q.setSequenceNo(1);
            q.setStem("测试题");
            q.setOptionA("A");
            q.setOptionB("B");
            q.setOptionC("C");
            q.setOptionD("D");
            q.setCorrectAnswer("A");
            q.setQuestionType(QuestionType.SINGLE);
            q.setScore(1);

            int rows = quizQuestionMapper.insert(q);
            assertThat(rows).isEqualTo(1);
            assertThat(q.getId()).isNotNull();

            QuizQuestion fetched = quizQuestionMapper.selectById(q.getId());
            assertThat(fetched).isNotNull();
            assertThat(fetched.getQuestionType()).isEqualTo(QuestionType.SINGLE);
            assertThat(fetched.getStem()).isEqualTo("测试题");
        }

        @Test
        @DisplayName("multitype correctAnswer(JSON 字符串) 可持久化")
        void shouldPersistMultiAnswer() {
            QuizQuestion q = new QuizQuestion();
            q.setQuizSetId(200L);
            q.setSequenceNo(1);
            q.setStem("多选题");
            q.setOptionA("A");
            q.setOptionB("B");
            q.setOptionC("C");
            q.setOptionD("D");
            q.setCorrectAnswer("[\"A\",\"C\"]");
            q.setQuestionType(QuestionType.MULTI);
            q.setScore(2);

            quizQuestionMapper.insert(q);

            QuizQuestion fetched = quizQuestionMapper.selectById(q.getId());
            assertThat(fetched.getCorrectAnswer()).isEqualTo("[\"A\",\"C\"]");
            assertThat(fetched.getQuestionType()).isEqualTo(QuestionType.MULTI);
        }
    }

    @Nested
    @DisplayName("复合唯一索引 (quiz_set_id + sequence_no)")
    class CompositeUniqueIndex {

        @Test
        @DisplayName("相同 quizSetId 下 sequence_no 重复插入应抛异常")
        void shouldReplicateDuplicateSequenceWithinSet() {
            QuizQuestion q1 = new QuizQuestion();
            q1.setQuizSetId(300L);
            q1.setSequenceNo(1);
            q1.setStem("Q1");
            q1.setCorrectAnswer("A");
            quizQuestionMapper.insert(q1);

            QuizQuestion q2 = new QuizQuestion();
            q2.setQuizSetId(300L);
            q2.setSequenceNo(1);
            q2.setStem("Q1 dup");
            q2.setCorrectAnswer("A");

            assertThrows(Exception.class, () -> quizQuestionMapper.insert(q2),
                    "相同 quiz_set_id + sequence_no 应被唯一索引阻止");
        }

        @Test
        @DisplayName("不同 quizSetId 下相同 sequence_no 应允许插入")
        void shouldAllowSameSequenceAcrossSets() {
            QuizQuestion q1 = new QuizQuestion();
            q1.setQuizSetId(310L);
            q1.setSequenceNo(1);
            q1.setStem("Set310 Q1");
            q1.setCorrectAnswer("A");

            QuizQuestion q2 = new QuizQuestion();
            q2.setQuizSetId(311L);
            q2.setSequenceNo(1);
            q2.setStem("Set311 Q1");
            q2.setCorrectAnswer("B");

            int r1 = quizQuestionMapper.insert(q1);
            int r2 = quizQuestionMapper.insert(q2);
            assertThat(r1).isEqualTo(1);
            assertThat(r2).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("questionType 枚举持久化")
    class QuestionTypePersistence {

        @Test
        @DisplayName("JUDGE 类型可正确存读")
        void shouldPersistJudgeType() {
            QuizQuestion q = new QuizQuestion();
            q.setQuizSetId(400L);
            q.setSequenceNo(1);
            q.setStem("判断题");
            q.setCorrectAnswer("A");
            q.setQuestionType(QuestionType.JUDGE);

            quizQuestionMapper.insert(q);

            QuizQuestion fetched = quizQuestionMapper.selectById(q.getId());
            assertThat(fetched.getQuestionType()).isEqualTo(QuestionType.JUDGE);
        }
    }
}
