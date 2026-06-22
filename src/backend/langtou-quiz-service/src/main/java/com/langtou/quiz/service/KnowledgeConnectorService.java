package com.langtou.quiz.service;

import com.langtou.quiz.dto.KnowledgeConnectionResult;
import com.langtou.quiz.entity.KnowledgeConnection;

import java.util.List;

public interface KnowledgeConnectorService {

    KnowledgeConnectionResult getConnections(Long questionId);

    KnowledgeConnectionResult getConnectionsByTopic(String topic);

    void addConnection(Long sourceQuestionId, Long targetQuestionId,
                      com.langtou.quiz.enums.ConnectionTypeEnum type);

    List<KnowledgeConnection> findRelatedQuestions(String topic, int limit);
}
