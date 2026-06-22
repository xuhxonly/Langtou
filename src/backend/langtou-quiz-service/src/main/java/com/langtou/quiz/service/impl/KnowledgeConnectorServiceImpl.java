package com.langtou.quiz.service.impl;

import com.langtou.quiz.dto.KnowledgeConnectionResult;
import com.langtou.quiz.entity.KnowledgeConnection;
import com.langtou.quiz.enums.ConnectionTypeEnum;
import com.langtou.quiz.mapper.KnowledgeConnectionMapper;
import com.langtou.quiz.service.KnowledgeConnectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeConnectorServiceImpl implements KnowledgeConnectorService {

    private final KnowledgeConnectionMapper knowledgeConnectionMapper;

    @Override
    public KnowledgeConnectionResult getConnections(Long questionId) {
        List<KnowledgeConnection> connections = knowledgeConnectionMapper.findBySourceQuestionId(questionId);
        return buildResult(questionId, null, connections);
    }

    @Override
    public KnowledgeConnectionResult getConnectionsByTopic(String topic) {
        List<KnowledgeConnection> connections = knowledgeConnectionMapper.findByTopic(topic);
        KnowledgeConnectionResult result = buildResult(null, topic, connections);
        if (connections.isEmpty()) {
            seedDefaultConnections(topic, result);
        }
        return result;
    }

    @Override
    @Transactional
    public void addConnection(Long sourceQuestionId, Long targetQuestionId, ConnectionTypeEnum type) {
        KnowledgeConnection conn = new KnowledgeConnection();
        conn.setSourceQuestionId(sourceQuestionId);
        conn.setTargetQuestionId(targetQuestionId);
        conn.setConnectionType(type);
        conn.setStrength(5);
        knowledgeConnectionMapper.insert(conn);
        log.info("add knowledge connection: source={}, target={}, type={}",
                sourceQuestionId, targetQuestionId, type);
    }

    @Override
    public List<KnowledgeConnection> findRelatedQuestions(String topic, int limit) {
        List<KnowledgeConnection> connections = knowledgeConnectionMapper.findByTopic(topic);
        if (connections.size() >= limit) {
            return connections.subList(0, limit);
        }
        List<KnowledgeConnection> typeBased = new ArrayList<>();
        for (ConnectionTypeEnum type : Arrays.asList(ConnectionTypeEnum.values())) {
            typeBased.addAll(knowledgeConnectionMapper.findByType(type, limit));
            if (typeBased.size() >= limit) {
                break;
            }
        }
        if (typeBased.size() >= limit) {
            return typeBased.subList(0, limit);
        }
        List<KnowledgeConnection> merged = new ArrayList<>(connections);
        for (KnowledgeConnection c : typeBased) {
            if (!merged.contains(c)) {
                merged.add(c);
            }
            if (merged.size() >= limit) {
                break;
            }
        }
        return merged;
    }

    private KnowledgeConnectionResult buildResult(Long sourceQuestionId, String topic,
                                                  List<KnowledgeConnection> connections) {
        KnowledgeConnectionResult result = new KnowledgeConnectionResult();
        result.setSourceQuestionId(sourceQuestionId);
        result.setTopic(topic);
        result.setExtensions(toItems(filterByType(connections, ConnectionTypeEnum.EXTENSION)));
        result.setChallenges(toItems(filterByType(connections, ConnectionTypeEnum.CHALLENGE)));
        result.setCrossDomains(toItems(filterByType(connections, ConnectionTypeEnum.CROSS_DOMAIN)));
        result.setApplications(toItems(filterByType(connections, ConnectionTypeEnum.APPLICATION)));
        result.setHistories(toItems(filterByType(connections, ConnectionTypeEnum.HISTORY)));
        return result;
    }

    private List<KnowledgeConnection> filterByType(List<KnowledgeConnection> connections,
                                                  ConnectionTypeEnum type) {
        if (connections == null) {
            return new ArrayList<>();
        }
        return connections.stream()
                .filter(c -> type.equals(c.getConnectionType()))
                .collect(Collectors.toList());
    }

    private List<KnowledgeConnectionResult.ConnectionItem> toItems(List<KnowledgeConnection> connections) {
        List<KnowledgeConnectionResult.ConnectionItem> items = new ArrayList<>();
        for (KnowledgeConnection c : connections) {
            KnowledgeConnectionResult.ConnectionItem item = new KnowledgeConnectionResult.ConnectionItem();
            item.setQuestionId(c.getTargetQuestionId());
            item.setType(c.getConnectionType());
            item.setStrength(c.getStrength());
            item.setTitle("Q#" + c.getTargetQuestionId());
            item.setDescription(c.getDescription());
            items.add(item);
        }
        return items;
    }

    private void seedDefaultConnections(String topic, KnowledgeConnectionResult result) {
        KnowledgeConnectionResult.ConnectionItem ext = new KnowledgeConnectionResult.ConnectionItem();
        ext.setType(ConnectionTypeEnum.EXTENSION);
        ext.setTitle("Extended reading about " + topic);
        ext.setDescription("Explore related knowledge points of " + topic);
        ext.setStrength(3);
        result.setExtensions(new ArrayList<>(List.of(ext)));

        KnowledgeConnectionResult.ConnectionItem challenge = new KnowledgeConnectionResult.ConnectionItem();
        challenge.setType(ConnectionTypeEnum.CHALLENGE);
        challenge.setTitle("Challenge variant of " + topic);
        challenge.setDescription("Higher difficulty questions on " + topic);
        challenge.setStrength(4);
        result.setChallenges(new ArrayList<>(List.of(challenge)));

        KnowledgeConnectionResult.ConnectionItem cross = new KnowledgeConnectionResult.ConnectionItem();
        cross.setType(ConnectionTypeEnum.CROSS_DOMAIN);
        cross.setTitle("Cross-domain link of " + topic);
        cross.setDescription("Associations of this topic with other subjects");
        cross.setStrength(2);
        result.setCrossDomains(new ArrayList<>(List.of(cross)));

        KnowledgeConnectionResult.ConnectionItem app = new KnowledgeConnectionResult.ConnectionItem();
        app.setType(ConnectionTypeEnum.APPLICATION);
        app.setTitle("Real-world application of " + topic);
        app.setDescription("Practical scenarios where " + topic + " is applied");
        app.setStrength(4);
        result.setApplications(new ArrayList<>(List.of(app)));

        KnowledgeConnectionResult.ConnectionItem hist = new KnowledgeConnectionResult.ConnectionItem();
        hist.setType(ConnectionTypeEnum.HISTORY);
        hist.setTitle("Historical development of " + topic);
        hist.setDescription("The historical context and evolution of " + topic);
        hist.setStrength(2);
        result.setHistories(new ArrayList<>(List.of(hist)));
    }
}
