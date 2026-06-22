package com.langtou.quiz.mapper;

import com.langtou.quiz.entity.QuizAttempt;
import com.langtou.quiz.enums.QuizAttemptStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisPlusTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MybatisPlusTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:langtou_quiz_attempt_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
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
@DisplayName("QuizAttemptMapper 集成测试")
class QuizAttemptMapperTest {

    @Autowired
    private QuizAttemptMapper quizAttemptMapper;

    @Nested
    @DisplayName("findByGameSessionId 状态过滤")
    class StatusFiltering {

        @Test
        @DisplayName("仅返回 IN_PROGRESS 状态的答题记录")
        void shouldFilterByInProgressStatus() {
            QuizAttempt attempt = quizAttemptMapper.findByGameSessionId(5004L);

            assertThat(attempt).isNotNull();
            assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("COMPLETED 状态的记录不应被 findByGameSessionId 命中")
        void shouldNotReturnCompletedOnes() {
            QuizAttempt attempt = quizAttemptMapper.findByGameSessionId(5001L);
            assertThat(attempt).isNull();
        }

        @Test
        @DisplayName("不存在的 gameSessionId 应返回 null")
        void shouldReturnNullWhenSessionNotExist() {
            QuizAttempt attempt = quizAttemptMapper.findByGameSessionId(9999L);
            assertThat(attempt).isNull();
        }
    }

    @Nested
    @DisplayName("CRUD 基础操作")
    class CrudOperations {

        @Test
        @DisplayName("insert + selectById 应一致")
        void shouldInsertAndSelect() {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuizSetId(1L);
            attempt.setUserId(9999L);
            attempt.setGameSessionId(7777L);
            attempt.setTotalQuestions(10);
            attempt.setStatus(QuizAttemptStatus.IN_PROGRESS);
            attempt.setVersion(0);

            int rows = quizAttemptMapper.insert(attempt);
            assertThat(rows).isEqualTo(1);
            assertThat(attempt.getId()).isNotNull();

            QuizAttempt fetched = quizAttemptMapper.selectById(attempt.getId());
            assertThat(fetched).isNotNull();
            assertThat(fetched.getGameSessionId()).isEqualTo(7777L);
            assertThat(fetched.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateById 应更新乐观锁 version")
        void shouldUpdateVersionOnUpdate() {
            QuizAttempt attempt = quizAttemptMapper.selectById(1L);
            assertThat(attempt).isNotNull();
            Integer originVersion = attempt.getVersion();

            attempt.setCorrectCount(9);
            attempt.setStatus(QuizAttemptStatus.COMPLETED);
            int rows = quizAttemptMapper.updateById(attempt);
            assertThat(rows).isEqualTo(1);

            QuizAttempt updated = quizAttemptMapper.selectById(1L);
            assertThat(updated.getStatus()).isEqualTo(QuizAttemptStatus.COMPLETED);
            assertThat(updated.getVersion()).isEqualTo(originVersion + 1);
        }
    }

    @Nested
    @DisplayName("唯一键约束 (game_session_id)")
    class UniqueKeyConstraint {

        @Test
        @DisplayName("重复 game_session_id 应抛异常")
        void shouldReplicateDuplicateGameSession() {
            QuizAttempt a1 = new QuizAttempt();
            a1.setQuizSetId(1L);
            a1.setUserId(100L);
            a1.setGameSessionId(8888L);
            a1.setStatus(QuizAttemptStatus.IN_PROGRESS);
            quizAttemptMapper.insert(a1);

            QuizAttempt a2 = new QuizAttempt();
            a2.setQuizSetId(1L);
            a2.setUserId(100L);
            a2.setGameSessionId(8888L);
            a2.setStatus(QuizAttemptStatus.IN_PROGRESS);

            assertThrows(Exception.class, () -> quizAttemptMapper.insert(a2),
                    "game_session_id 唯一索引应阻止重复");
        }
    }
}
