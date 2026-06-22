package com.langtou.quiz.config;

import com.langtou.quiz.enums.DegradeLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuizProperties 配置绑定测试")
class QuizPropertiesTest {

    private QuizProperties loadFromYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties properties = yaml.getObject();
        assertThat(properties).isNotNull();

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        return binder.bind("quiz", Bindable.of(QuizProperties.class)).get();
    }

    @Nested
    @DisplayName("默认值加载")
    class DefaultValues {

        @Test
        @DisplayName("Question.defaultCount 应为 10")
        void defaultQuestionCountShouldBe10() {
            assertThat(loadFromYaml().getQuestion().getDefaultCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("Question.minCount 应为 5")
        void minCountShouldBe5() {
            assertThat(loadFromYaml().getQuestion().getMinCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Question.maxCount 应为 12")
        void maxCountShouldBe12() {
            assertThat(loadFromYaml().getQuestion().getMaxCount()).isEqualTo(12);
        }

        @Test
        @DisplayName("Question.perQuestionSeconds 应为 60")
        void perQuestionSecondsShouldBe60() {
            assertThat(loadFromYaml().getQuestion().getPerQuestionSeconds()).isEqualTo(60);
        }

        @Test
        @DisplayName("Question.passingScore 应为 7")
        void passingScoreShouldBe7() {
            assertThat(loadFromYaml().getQuestion().getPassingScore()).isEqualTo(7);
        }

        @Test
        @DisplayName("Revive.maxPerGame 应为 2")
        void maxReviveShouldBe2() {
            assertThat(loadFromYaml().getRevive().getMaxPerGame()).isEqualTo(2);
        }

        @Test
        @DisplayName("Revive.priceFen 应为 99")
        void revivePriceShouldBe99() {
            assertThat(loadFromYaml().getRevive().getPriceFen()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("通过代码直接绑定（验证默认值回退）")
    class DirectBinding {

        @Test
        @DisplayName("空绑定时应使用 Java 默认值")
        void shouldUseJavaDefaultValuesWhenEmpty() {
            ConfigurationPropertySource source = new MapConfigurationPropertySource(new Properties());
            Binder binder = new Binder(source);
            QuizProperties props = binder.bind("quiz", Bindable.of(QuizProperties.class)).get();

            assertThat(props.getQuestion().getDefaultCount()).isEqualTo(10);
            assertThat(props.getQuestion().getPassingScore()).isEqualTo(7);
            assertThat(props.getRevive().getMaxPerGame()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("DegradeLevel 枚举绑定")
    class DegradeLevelBinding {

        @Test
        @DisplayName("Degrade.level 应绑定为 NONE")
        void degradeLevelShouldBindToNone() {
            assertThat(loadFromYaml().getDegrade().getLevel()).isEqualTo(DegradeLevel.NONE);
        }

        @Test
        @DisplayName("DegradeLevel 枚举所有值均可被绑定")
        void allDegradeLevelsShouldBeValid() {
            assertThat(DegradeLevel.values())
                    .containsExactly(DegradeLevel.NONE, DegradeLevel.LOW, DegradeLevel.MEDIUM, DegradeLevel.HIGH);
        }

        @Test
        @DisplayName("显式绑定 LOW 枚举值应正确")
        void shouldBindLowExplicitly() {
            Properties props = new Properties();
            props.setProperty("quiz.degrade.level", "LOW");
            ConfigurationPropertySource source = new MapConfigurationPropertySource(props);
            Binder binder = new Binder(source);
            QuizProperties qp = binder.bind("quiz", Bindable.of(QuizProperties.class)).get();
            assertThat(qp.getDegrade().getLevel()).isEqualTo(DegradeLevel.LOW);
        }

        @Test
        @DisplayName("显式绑定 HIGH 枚举值应正确")
        void shouldBindHighExplicitly() {
            Properties props = new Properties();
            props.setProperty("quiz.degrade.level", "HIGH");
            ConfigurationPropertySource source = new MapConfigurationPropertySource(props);
            Binder binder = new Binder(source);
            QuizProperties qp = binder.bind("quiz", Bindable.of(QuizProperties.class)).get();
            assertThat(qp.getDegrade().getLevel()).isEqualTo(DegradeLevel.HIGH);
        }
    }

    @Nested
    @DisplayName("边界值")
    class BoundaryValues {

        @Test
        @DisplayName("minCount 应小于等于 maxCount")
        void minCountShouldNotExceedMaxCount() {
            QuizProperties qp = loadFromYaml();
            assertThat(qp.getQuestion().getMinCount())
                    .isLessThanOrEqualTo(qp.getQuestion().getMaxCount());
        }

        @Test
        @DisplayName("defaultCount 应在 [minCount, maxCount] 范围内")
        void defaultCountShouldBeInRange() {
            QuizProperties qp = loadFromYaml();
            int def = qp.getQuestion().getDefaultCount();
            int min = qp.getQuestion().getMinCount();
            int max = qp.getQuestion().getMaxCount();
            assertThat(def).isBetween(min, max);
        }
    }
}
