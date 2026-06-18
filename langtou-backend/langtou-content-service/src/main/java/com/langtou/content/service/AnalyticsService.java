package com.langtou.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.content.dto.AnalyticsEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 埋点事件处理服务
 * MVP阶段：同时写入Kafka topic和MySQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String KAFKA_TOPIC = "analytics_events";
    private static final String INSERT_SQL = """
            INSERT INTO analytics_event (id, user_id, event_name, properties, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 处理批量埋点事件
     *
     * @param events 事件列表
     */
    public void processBatchEvents(List<AnalyticsEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        log.info("收到批量埋点事件: {} 条", events.size());

        for (AnalyticsEventDTO event : events) {
            try {
                processSingleEvent(event);
            } catch (Exception e) {
                log.error("处理埋点事件失败: eventId={}, eventName={}", event.getEventId(), event.getEventName(), e);
            }
        }
    }

    /**
     * 处理单条埋点事件
     */
    private void processSingleEvent(AnalyticsEventDTO event) {
        // 1. 写入Kafka（异步，供实时计算消费）
        sendToKafka(event);

        // 2. 写入MySQL（MVP阶段同步写入，后续可改为异步批量）
        writeToMySQL(event);
    }

    /**
     * 将事件发送到Kafka topic
     */
    private void sendToKafka(AnalyticsEventDTO event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KAFKA_TOPIC, event.getUserId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("发送埋点事件到Kafka失败: eventId={}", event.getEventId(), ex);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("序列化埋点事件失败: eventId={}", event.getEventId(), e);
        }
    }

    /**
     * 将事件写入MySQL analytics_event表
     */
    private void writeToMySQL(AnalyticsEventDTO event) {
        try {
            String propertiesJson = "{}";
            if (event.getProperties() != null && !event.getProperties().isEmpty()) {
                propertiesJson = objectMapper.writeValueAsString(event.getProperties());
            }

            // 生成数据库ID（使用eventId）
            String id = event.getEventId();

            // 解析时间戳
            java.sql.Timestamp createdAt;
            if (event.getTimestamp() != null && event.getTimestamp() > 0) {
                createdAt = new java.sql.Timestamp(event.getTimestamp());
            } else {
                createdAt = java.sql.Timestamp.from(Instant.now());
            }

            jdbcTemplate.update(
                    INSERT_SQL,
                    id,
                    event.getUserId(),
                    event.getEventName(),
                    propertiesJson,
                    createdAt
            );
        } catch (Exception e) {
            log.error("写入埋点事件到MySQL失败: eventId={}", event.getEventId(), e);
        }
    }
}
