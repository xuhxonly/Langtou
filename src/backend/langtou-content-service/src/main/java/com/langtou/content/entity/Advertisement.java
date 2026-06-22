package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 广告实体
 */
@Data
@TableName("advertisement")
public class Advertisement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long advertiserId;

    private String title;

    private String imageUrl;

    private String targetUrl;

    /**
     * 广告类型：feed-信息流 banner-横幅 splash-开屏
     */
    private String adType;

    /**
     * 广告位权重（越大优先级越高）
     */
    private Integer position;

    /**
     * 状态：0-下架 1-投放中 2-审核中
     */
    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal budget;

    private Integer impressions;

    private Integer clicks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
