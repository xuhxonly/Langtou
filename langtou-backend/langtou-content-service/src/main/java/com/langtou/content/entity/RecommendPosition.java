package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 推荐位实体
 */
@Data
@TableName(value = "recommend_positions", autoResultMap = true)
public class RecommendPosition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 推荐位类型: HOME_BANNER/DISCOVER/TOPIC_PAGE/SEARCH/ONBOARDING
     */
    private String positionType;

    /**
     * 展示标题
     */
    private String title;

    /**
     * 内容配置(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    /**
     * 展示图片URL
     */
    private String imageUrl;

    /**
     * 跳转链接
     */
    private String linkUrl;

    /**
     * 排序(越大越靠前)
     */
    private Integer sortOrder;

    /**
     * 展示开始时间
     */
    private LocalDateTime startTime;

    /**
     * 展示结束时间
     */
    private LocalDateTime endTime;

    /**
     * 状态: ACTIVE/INACTIVE
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
