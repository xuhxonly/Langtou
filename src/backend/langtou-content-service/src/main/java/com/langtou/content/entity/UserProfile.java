package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户画像实体
 * 存储用户兴趣标签、行为特征、人口统计特征等个性化数据
 */
@Data
@TableName(value = "user_profile", autoResultMap = true)
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 兴趣标签及权重（JSON数组）
     * 格式: [{"tag": "美食", "weight": 0.85}, {"tag": "旅行", "weight": 0.72}]
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Double> interestTags;

    /**
     * 行为特征（JSON）
     * 活跃时段 / 内容偏好 / 互动倾向
     * 格式: {"activeHours": ["20:00-23:00"], "contentPreference": [1, 2], "interactionTendency": 0.65}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> behaviorFeatures;

    /**
     * 人口统计特征（JSON）
     * 年龄 / 性别 / 地域 / 设备
     * 格式: {"age": 25, "gender": 1, "region": "上海", "device": "iOS"}
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> demographicFeatures;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
