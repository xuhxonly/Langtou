package com.langtou.ad.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "recommend_positions", autoResultMap = true)
public class RecommendPosition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String positionType;

    private String title;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;

    private String imageUrl;

    private String linkUrl;

    private Integer sortOrder;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
