package com.langtou.quiz.config;

import com.langtou.quiz.enums.DegradeLevel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "quiz")
public class QuizProperties {

    private Question question = new Question();
    private Revive revive = new Revive();
    private Degrade degrade = new Degrade();

    @Data
    public static class Question {
        private int defaultCount = 10;
        private int minCount = 5;
        private int maxCount = 12;
        private int perQuestionSeconds = 60;
        private int lifePerQuestion = 1;
        private int passingScore = 7;
    }

    @Data
    public static class Revive {
        private int maxPerGame = 2;
        private int priceFen = 99;
    }

    @Data
    public static class Degrade {
        private DegradeLevel level = DegradeLevel.NONE;
    }
}