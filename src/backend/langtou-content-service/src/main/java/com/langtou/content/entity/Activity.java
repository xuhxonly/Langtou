package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 活动实体
 */
@Data
@TableName(value = "activities", autoResultMap = true)
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String coverUrl;

    /**
     * 创建者/运营人员ID
     */
    private Long creatorId;

    /**
     * 活动类型: CHALLENGE/TOPIC/EVENT
     */
    private String type;

    /**
     * 状态: DRAFT/PENDING_REVIEW/ONLINE/ENDED/REJECTED
     */
    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 参与规则(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> participationRules;

    /**
     * 奖励配置(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> rewardConfig;

    /**
     * 参与人数(冗余)
     */
    private Integer participantCount;

    /**
     * 参与笔记数(冗余)
     */
    private Integer noteCount;

    /**
     * 总浏览量(冗余)
     */
    private Long totalViews;

    /**
     * 总互动量(冗余)
     */
    private Long totalInteractions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
