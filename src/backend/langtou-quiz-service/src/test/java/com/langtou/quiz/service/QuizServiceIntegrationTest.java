package com.langtou.quiz.service;

import com.langtou.quiz.QuizServiceApplication;
import com.langtou.quiz.TestRedisConfiguration;
import com.langtou.quiz.dto.QuizSubmitRequest;
import com.langtou.quiz.dto.QuizSubmitRequest.AnswerItem;
import com.langtou.quiz.entity.QuizAttempt;
import com.langtou.quiz.entity.QuizQuestion;
import com.langtou.quiz.entity.QuizSet;
import com.langtou.quiz.enums.QuizAttemptStatus;
import com.langtou.quiz.enums.QuizSetStatus;
import com.langtou.quiz.mapper.QuizAttemptMapper;
import com.langtou.quiz.mapper.QuizQuestionMapper;
import com.langtou.quiz.mapper.QuizSetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = QuizServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:langtou_quiz_integration_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql",
                "spring.sql.init.data-locations=classpath:data.sql",
                "spring.flyway.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.discovery.register-enabled=false",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.id-type=auto"
        }
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestRedisConfiguration.class})
@DisplayName("QuizService 集成测试")
class QuizServiceIntegrationTest {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizSetMapper quizSetMapper;

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Autowired
    private QuizAttemptMapper quizAttemptMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        reset(stringRedisTemplate);
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
    }

    private Long prepareQuizSet(int questionCount) {
        QuizSet set = new QuizSet();
        set.setNoteId(System.currentTimeMillis());
        set.setCreatorId(2001L);
        set.setTitle("测试题库");
        set.setStatus(QuizSetStatus.READY);
        set.setQuestionCount(questionCount);
        set.setSource("TEST");
        set.setPromptHash("hash_" + System.nanoTime());
        quizSetMapper.insert(set);

        for (int i = 1; i <= questionCount; i++) {
            QuizQuestion q = new QuizQuestion();
            q.setQuizSetId(set.getId());
            q.setSequenceNo(i);
            q.setStem("Q" + i);
            q.setOptionA("A");
            q.setOptionB("B");
            q.setOptionC("C");
            q.setOptionD("D");
            q.setCorrectAnswer("A");
            q.setScore(1);
            quizQuestionMapper.insert(q);
        }
        return set.getId();
    }

    private QuizSubmitRequest buildRequest(QuizAttempt attempt, List<AnswerItem> answers, int duration) {
        QuizSubmitRequest request = new QuizSubmitRequest();
        request.setAttemptId(attempt.getId());
        request.setAnswers(answers);
        request.setDurationSeconds(duration);
        return request;
    }

    private List<AnswerItem> buildAllCorrectAnswers(int count) {
        List<AnswerItem> answers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            AnswerItem item = new AnswerItem();
            item.setSequenceNo(i);
            item.setSelected("A");
            answers.add(item);
        }
        return answers;
    }

    private List<AnswerItem> buildAllWrongAnswers(int count) {
        List<AnswerItem> answers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            AnswerItem item = new AnswerItem();
            item.setSequenceNo(i);
            item.setSelected("B");
            answers.add(item);
        }
        return answers;
    }

    @Nested
    @DisplayName("场景 1：正常答题 → 提交 → 计分 → 通关")
    class NormalFlow {

        @Test
        @DisplayName("全部答对，分数等于题目数，状态 COMPLETED，passed=true")
        void shouldScoreAndPassWhenAllCorrect() {
            Long quizSetId = prepareQuizSet(10);

            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3001L, 9001L);
            assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.IN_PROGRESS);

            Map<String, Object> result = quizService.submitAttempt(
                    buildRequest(attempt, buildAllCorrectAnswers(10), 60));

            assertThat(result).containsEntry("score", 10);
            assertThat(result).containsEntry("correctCount", 10);
            assertThat(result).containsEntry("passed", true);

            QuizAttempt finished = quizAttemptMapper.selectById(attempt.getId());
            assertThat(finished.getStatus()).isEqualTo(QuizAttemptStatus.COMPLETED);
            assertThat(finished.getPassed()).isTrue();
        }

        @Test
        @DisplayName("答对不足及格线，passed=false")
        void shouldFailWhenBelowPassingScore() {
            Long quizSetId = prepareQuizSet(10);

            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3002L, 9002L);

            Map<String, Object> result = quizService.submitAttempt(
                    buildRequest(attempt, buildAllWrongAnswers(10), 90));

            assertThat(result).containsEntry("score", 0);
            assertThat(result).containsEntry("passed", false);
        }

        @Test
        @DisplayName("答对 7 题刚好达到及格线，passed=true")
        void shouldPassAtBoundaryScore() {
            Long quizSetId = prepareQuizSet(10);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3010L, 9010L);

            List<AnswerItem> answers = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                AnswerItem item = new AnswerItem();
                item.setSequenceNo(i);
                item.setSelected(i <= 7 ? "A" : "B");
                answers.add(item);
            }

            Map<String, Object> result = quizService.submitAttempt(
                    buildRequest(attempt, answers, 80));

            assertThat(result).containsEntry("score", 7);
            assertThat(result).containsEntry("passed", true);
        }
    }

    @Nested
    @DisplayName("场景 2：超时答题 → 应拒绝")
    class TimeoutFlow {

        @Test
        @DisplayName("用时超过上限，抛 IllegalStateException")
        void shouldRejectWhenDurationExceedsLimit() {
            Long quizSetId = prepareQuizSet(10);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3003L, 9003L);

            assertThatThrownBy(() -> quizService.submitAttempt(
                    buildRequest(attempt, buildAllCorrectAnswers(10), 10000)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("超时");
        }

        @Test
        @DisplayName("不填 durationSeconds 时不应触发超时校验")
        void shouldAllowNullDuration() {
            Long quizSetId = prepareQuizSet(10);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3007L, 9007L);

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAttemptId(attempt.getId());
            request.setAnswers(buildAllCorrectAnswers(10));
            request.setDurationSeconds(null);

            Map<String, Object> result = quizService.submitAttempt(request);
            assertThat(result).containsEntry("score", 10);
        }
    }

    @Nested
    @DisplayName("场景 3：并发提交 → 乐观锁应生效")
    class OptimisticLockFlow {

        @Test
        @DisplayName("同一 version 下并发提交，第二次应被乐观锁拦截")
        void shouldRejectConcurrentSubmit() {
            Long quizSetId = prepareQuizSet(5);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3004L, 9004L);
            int originVersion = attempt.getVersion();

            attempt.setVersion(originVersion);
            attempt.setStatus(QuizAttemptStatus.COMPLETED);
            attempt.setScore(5);
            attempt.setCorrectCount(5);
            attempt.setPassed(true);
            int firstUpdate = quizAttemptMapper.updateById(attempt);
            assertThat(firstUpdate).isEqualTo(1);

            QuizAttempt staleCopy = new QuizAttempt();
            staleCopy.setId(attempt.getId());
            staleCopy.setVersion(originVersion);
            staleCopy.setScore(3);
            staleCopy.setCorrectCount(3);
            staleCopy.setStatus(QuizAttemptStatus.COMPLETED);
            staleCopy.setPassed(false);
            int secondUpdate = quizAttemptMapper.updateById(staleCopy);

            assertThat(secondUpdate)
                    .as("乐观锁应阻止 stale 版本写入")
                    .isEqualTo(0);

            QuizAttempt finalAttempt = quizAttemptMapper.selectById(attempt.getId());
            assertThat(finalAttempt.getScore()).isEqualTo(5);
            assertThat(finalAttempt.getPassed()).isTrue();
        }

        @Test
        @DisplayName("乐观锁版本递增正确")
        void versionShouldIncrementOnEachUpdate() {
            Long quizSetId = prepareQuizSet(3);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3008L, 9008L);
            Integer versionAfterStart = quizAttemptMapper.selectById(attempt.getId()).getVersion();

            attempt.setStatus(QuizAttemptStatus.COMPLETED);
            attempt.setScore(3);
            quizAttemptMapper.updateById(attempt);

            Integer versionAfterSubmit = quizAttemptMapper.selectById(attempt.getId()).getVersion();
            assertThat(versionAfterSubmit).isGreaterThan(versionAfterStart);
        }
    }

    @Nested
    @DisplayName("场景 4：重复提交 → 幂等锁应拒绝第二次")
    class IdempotentLockFlow {

        @Test
        @DisplayName("Redis setIfAbsent 返回 false 时应拒绝重复提交")
        void shouldRejectDuplicateSubmit() {
            Long quizSetId = prepareQuizSet(3);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3005L, 9005L);

            org.springframework.data.redis.core.ValueOperations<String, String> valueOps =
                    mock(org.springframework.data.redis.core.ValueOperations.class);
            when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

            quizService.submitAttempt(buildRequest(attempt, buildAllCorrectAnswers(3), 30));

            when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);

            QuizSubmitRequest second = buildRequest(attempt, buildAllCorrectAnswers(3), 30);

            assertThatThrownBy(() -> quizService.submitAttempt(second))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("幂等锁");
        }

        @Test
        @DisplayName("Attempt 不是 IN_PROGRESS 时应拒绝提交")
        void shouldRejectNonProgressAttempt() {
            QuizAttempt completedAttempt = quizAttemptMapper.selectById(1L);
            assertThat(completedAttempt.getStatus()).isEqualTo(QuizAttemptStatus.COMPLETED);

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAttemptId(completedAttempt.getId());
            request.setAnswers(Arrays.asList(new AnswerItem()));

            assertThatThrownBy(() -> quizService.submitAttempt(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in progress");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("提交不存在的 Attempt 应抛异常")
        void shouldRejectNonExistingAttempt() {
            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAttemptId(999999L);
            request.setAnswers(Arrays.asList(new AnswerItem()));

            assertThatThrownBy(() -> quizService.submitAttempt(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Attempt not found");
        }

        @Test
        @DisplayName("提交 sequenceNo 超出题目范围应抛异常")
        void shouldRejectOutOfRangeSequence() {
            Long quizSetId = prepareQuizSet(3);
            QuizAttempt attempt = quizService.startAttempt(quizSetId, 3006L, 9006L);

            AnswerItem bad = new AnswerItem();
            bad.setSequenceNo(999);
            bad.setSelected("A");

            QuizSubmitRequest request = new QuizSubmitRequest();
            request.setAttemptId(attempt.getId());
            request.setAnswers(Arrays.asList(bad));
            request.setDurationSeconds(10);

            assertThatThrownBy(() -> quizService.submitAttempt(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sequenceNo 超范围");
        }

        @Test
        @DisplayName("startAttempt 不存在的 quizSetId 应抛异常")
        void shouldRejectInvalidQuizSetOnStart() {
            assertThatThrownBy(() -> quizService.startAttempt(999999L, 3099L, 9099L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
