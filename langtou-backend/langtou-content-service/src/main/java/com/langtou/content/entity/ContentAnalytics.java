package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内容分析实体
 */
@Data
@TableName("content_analytics")
public class ContentAnalytics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    /**
     * 流量来源: DIRECT/SEARCH/RECOMMEND/HASHTAG/PROFILE/EXTERNAL
     */
    private String viewSource;

    private Long viewCount;

    private Long uniqueViewCount;

    /**
     * 平均阅读时长(秒)
     */
    private Integer avgReadDuration;

    /**
     * 点击率
     */
    private BigDecimal clickThroughRate;

    private Long shareCount;

    private Long saveCount;

    private LocalDate date;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
