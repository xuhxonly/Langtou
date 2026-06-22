package com.langtou.quiz.dto;

import com.langtou.quiz.enums.ConnectionTypeEnum;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeConnectionResult {

    private Long sourceQuestionId;

    private String topic;

    private List<ConnectionItem> extensions;

    private List<ConnectionItem> challenges;

    private List<ConnectionItem> crossDomains;

    private List<ConnectionItem> applications;

    private List<ConnectionItem> histories;

    @Data
    public static class ConnectionItem {

        private Long questionId;

        private String title;

        private String description;

        private ConnectionTypeEnum type;

        private Integer strength;

        private String subject;
    }
}
