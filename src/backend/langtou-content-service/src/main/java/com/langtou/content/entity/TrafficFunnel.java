package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 转化漏斗实体
 */
@Data
@TableName("traffic_funnel")
public class TrafficFunnel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contentId;

    /**
     * 曝光次数
     */
    private Long impressionCount;

    /**
     * 点击次数
     */
    private Long clickCount;

    /**
     * 阅读次数
     */
    private Long readCount;

    /**
     * 互动次数(点赞+评论+收藏)
     */
    private Long interactCount;

    /**
     * 分享次数
     */
    private Long shareCount;

    private LocalDate date;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
