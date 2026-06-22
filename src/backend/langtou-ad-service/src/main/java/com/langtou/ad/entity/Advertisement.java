package com.langtou.ad.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("advertisement")
public class Advertisement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long advertiserId;

    private String title;

    private String imageUrl;

    private String targetUrl;

    private String adType;

    private Integer position;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal budget;

    private Integer impressions;

    private Integer clicks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
