package com.langtou.quiz.mapper;

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
                "spring.datasource.url=jdbc:h2:mem:langtou_quiz_set_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
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
@DisplayName("QuizSetMapper 集成测试")
class QuizSetMapperTest {

    @Autowired
    private QuizSetMapper quizSetMapper;

    @Nested
    @DisplayName("findLatestByNoteId 排序正确性")
    class FindLatestOrdering {

        @Test
        @DisplayName("同一 noteId 下应返回最新（id 最大）且状态为 READY/PUBLISHED 的记录")
        void shouldReturnLatestReadyOrPublished() {
            Long noteId = 1001L;

            var latest = quizSetMapper.findLatestByNoteId(noteId);

            assertThat(latest).isNotNull();
            assertThat(latest.getNoteId()).isEqualTo(noteId);
            assertThat(latest.getId())
                    .as("应为 ID 最大的记录（V2）")
                    .isEqualTo(4L);
            assertThat(latest.getStatus()).isIn(
                    com.langtou.quiz.enums.QuizSetStatus.READY,
                    com.langtou.quiz.enums.QuizSetStatus.PUBLISHED
            );
        }

        @Test
        @DisplayName("无匹配 noteId 时应返回 null")
        void shouldReturnNullWhenNoteNotExist() {
            var latest = quizSetMapper.findLatestByNoteId(9999L);
            assertThat(latest).isNull();
        }

        @Test
        @DisplayName("仅返回 READY/PUBLISHED 状态，排除 FAILED/EXPIRED")
        void shouldExcludeInvalidStatuses() {
            com.langtou.quiz.entity.QuizSet set = new com.langtou.quiz.entity.QuizSet();
            set.setNoteId(9001L);
            set.setCreatorId(2001L);
            set.setTitle("FAILED_SET");
            set.setStatus(com.langtou.quiz.enums.QuizSetStatus.FAILED);
            quizSetMapper.insert(set);

            var latest = quizSetMapper.findLatestByNoteId(9001L);
            assertThat(latest).isNull();
        }
    }

    @Nested
    @DisplayName("CRUD 基础操作")
    class CrudOperations {

        @Test
        @DisplayName("insert 与 selectById 应一致")
        void shouldInsertAndSelectById() {
            com.langtou.quiz.entity.QuizSet set = new com.langtou.quiz.entity.QuizSet();
            set.setNoteId(8001L);
            set.setCreatorId(2001L);
            set.setTitle("新建 Quiz");
            set.setStatus(com.langtou.quiz.enums.QuizSetStatus.READY);
            set.setQuestionCount(5);

            int rows = quizSetMapper.insert(set);
            assertThat(rows).isEqualTo(1);
            assertThat(set.getId()).isNotNull();

            com.langtou.quiz.entity.QuizSet fetched = quizSetMapper.selectById(set.getId());
            assertThat(fetched).isNotNull();
            assertThat(fetched.getTitle()).isEqualTo("新建 Quiz");
        }

        @Test
        @DisplayName("updateById 应正确更新记录")
        void shouldUpdateById() {
            com.langtou.quiz.entity.QuizSet set = quizSetMapper.selectById(1L);
            assertThat(set).isNotNull();

            set.setTitle("更新后的标题");
            int rows = quizSetMapper.updateById(set);
            assertThat(rows).isEqualTo(1);

            com.langtou.quiz.entity.QuizSet updated = quizSetMapper.selectById(1L);
            assertThat(updated.getTitle()).isEqualTo("更新后的标题");
        }
    }

    @Nested
    @DisplayName("唯一键约束（note_id + status）")
    class UniqueKeyConstraint {

        @Test
        @DisplayName("同一 noteId 与 status 组合重复插入应抛出异常")
        void shouldRejectDuplicateNoteStatus() {
            com.langtou.quiz.entity.QuizSet duplicate = new com.langtou.quiz.entity.QuizSet();
            duplicate.setNoteId(1001L);
            duplicate.setCreatorId(2001L);
            duplicate.setTitle("重复记录");
            duplicate.setStatus(com.langtou.quiz.enums.QuizSetStatus.PUBLISHED);

            assertThrows(Exception.class, () -> {
                quizSetMapper.insert(duplicate);
            }, "note_id + status 唯一索引应阻止重复");
        }

        @Test
        @DisplayName("相同 noteId 不同 status 应允许插入")
        void shouldAllowSameNoteIdWithDifferentStatus() {
            com.langtou.quiz.entity.QuizSet newOne = new com.langtou.quiz.entity.QuizSet();
            newOne.setNoteId(1001L);
            newOne.setCreatorId(2001L);
            newOne.setTitle("新状态");
            newOne.setStatus(com.langtou.quiz.enums.QuizSetStatus.FAILED);

            int rows = quizSetMapper.insert(newOne);
            assertThat(rows).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("tags JSON 序列化持久化")
    class TagsPersistence {

        @Test
        @DisplayName("tags 通过 JacksonTypeHandler 可正确存读")
        void shouldPersistTagsAsJson() {
            java.util.List<String> tags = java.util.Arrays.asList("美妆", "护肤", "入门");

            com.langtou.quiz.entity.QuizSet set = new com.langtou.quiz.entity.QuizSet();
            set.setNoteId(7001L);
            set.setCreatorId(2001L);
            set.setTitle("带 Tags 测试");
            set.setStatus(com.langtou.quiz.enums.QuizSetStatus.READY);
            set.setTags(tags);

            quizSetMapper.insert(set);

            com.langtou.quiz.entity.QuizSet fetched = quizSetMapper.selectById(set.getId());
            assertThat(fetched.getTags()).containsExactly("美妆", "护肤", "入门");
        }
    }
}
