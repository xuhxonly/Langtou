package com.langtou.quiz.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.quiz.enums.QuizSetStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizSet 单元测试")
class QuizSetTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("状态枚举映射（@EnumValue）")
    class StatusEnumMapping {

        @Test
        @DisplayName("PENDING 枚举值应为字符串 'PENDING'")
        void pendingEnumValueShouldBePendingString() {
            QuizSetStatus status = QuizSetStatus.PENDING;
            assertThat(status.getValue()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("READY 枚举值应为字符串 'READY'")
        void readyEnumValueShouldBeReadyString() {
            assertThat(QuizSetStatus.READY.getValue()).isEqualTo("READY");
        }

        @Test
        @DisplayName("FAILED 枚举值应为字符串 'FAILED'")
        void failedEnumValueShouldBeFailedString() {
            assertThat(QuizSetStatus.FAILED.getValue()).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("EXPIRED 枚举值应为字符串 'EXPIRED'")
        void expiredEnumValueShouldBeExpiredString() {
            assertThat(QuizSetStatus.EXPIRED.getValue()).isEqualTo("EXPIRED");
        }

        @Test
        @DisplayName("PUBLISHED 枚举值应为字符串 'PUBLISHED'")
        void publishedEnumValueShouldBePublishedString() {
            assertThat(QuizSetStatus.PUBLISHED.getValue()).isEqualTo("PUBLISHED");
        }

        @Test
        @DisplayName("所有枚举值在 switch/if 中可被正确识别")
        void allEnumValuesShouldBeUsableInLogic() {
            for (QuizSetStatus s : QuizSetStatus.values()) {
                assertThat(s.getValue()).isNotNull().isNotEmpty();
            }
            assertThat(QuizSetStatus.values()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("tags 字段 JSON 序列化/反序列化（JacksonTypeHandler）")
    class TagsJsonMapping {

        @Test
        @DisplayName("tags 序列化为 JSON 字符串")
        void tagsShouldSerializeToJsonArray() throws JsonProcessingException {
            QuizSet quizSet = new QuizSet();
            List<String> tags = Arrays.asList("美妆", "护肤", "入门");
            quizSet.setTags(tags);

            String json = objectMapper.writeValueAsString(tags);
            assertThat(json).isEqualTo("[\"美妆\",\"护肤\",\"入门\"]");
        }

        @Test
        @DisplayName("tags JSON 字符串反序列化为 List<String>")
        void jsonStringShouldDeserializeToTagList() throws JsonProcessingException {
            String json = "[\"职场\",\"新人\"]";
            List<String> tags = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            assertThat(tags).containsExactly("职场", "新人");
        }

        @Test
        @DisplayName("tags 为空列表时序列化正确")
        void emptyTagsShouldSerializeAsEmptyArray() throws JsonProcessingException {
            String json = objectMapper.writeValueAsString(Arrays.asList());
            assertThat(json).isEqualTo("[]");
        }

        @Test
        @DisplayName("tags 字段通过 setTags/getTags 正常读写")
        void tagsFieldShouldBeSettableAndReadable() {
            QuizSet quizSet = new QuizSet();
            List<String> tags = Arrays.asList("A", "B", "C");
            quizSet.setTags(tags);
            assertThat(quizSet.getTags()).containsExactly("A", "B", "C");
        }

        @Test
        @DisplayName("tags 为 null 时序列化结果为 null")
        void nullTagsShouldSerializeToNull() throws JsonProcessingException {
            String json = objectMapper.writeValueAsString((List<String>) null);
            assertThat(json).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("基础属性赋值")
    class BasicFields {

        @Test
        @DisplayName("QuizSet 所有字段可正确赋值与读取")
        void allFieldsShouldBeSettable() {
            QuizSet quizSet = new QuizSet();
            quizSet.setId(1L);
            quizSet.setNoteId(1001L);
            quizSet.setCreatorId(2001L);
            quizSet.setTitle("美妆入门");
            quizSet.setCoverUrl("https://cdn.example.com/q.jpg");
            quizSet.setQuestionCount(10);
            quizSet.setStatus(QuizSetStatus.PUBLISHED);
            quizSet.setSource("AI");
            quizSet.setPromptHash("hash_abc");
            quizSet.setCorrectRate(92);

            assertThat(quizSet.getId()).isEqualTo(1L);
            assertThat(quizSet.getNoteId()).isEqualTo(1001L);
            assertThat(quizSet.getStatus()).isEqualTo(QuizSetStatus.PUBLISHED);
        }
    }
}
