package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 作弊举报实体
 */
@Data
@TableName(value = "fraud_reports", autoResultMap = true)
public class FraudReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 举报人用户ID
     */
    private Long userId;

    /**
     * 作弊类型: LIKE_SPAM/COMMENT_SPAM/FOLLOW_SPAM/CONTENT_DUPLICATE/ACCOUNT_ANOMALY
     */
    private String fraudType;

    /**
     * 严重程度: LOW/MEDIUM/HIGH/CRITICAL
     */
    private String severity;

    /**
     * 举报描述
     */
    private String description;

    /**
     * 证据(JSON格式，存储截图URL、数据快照等)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> evidence;

    /**
     * 处理状态: PENDING/CONFIRMED/DISMISSED
     */
    private String status;

    /**
     * 举报时间
     */
    private LocalDateTime createdAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 处理人(管理员)ID
     */
    private Long processorId;
}
