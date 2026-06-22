package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创作者每日统计实体
 */
@Data
@TableName("creator_daily_stats")
public class CreatorDailyStats {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    /**
     * 新增粉丝
     */
    private Integer newFollowers;

    /**
     * 取关粉丝
     */
    private Integer unfollowers;

    /**
     * 总粉丝数
     */
    private Integer totalFollowers;

    /**
     * 发布内容数
     */
    private Integer contentCount;

    /**
     * 总浏览量
     */
    private Long totalViews;

    /**
     * 总点赞数
     */
    private Long totalLikes;

    /**
     * 总评论数
     */
    private Long totalComments;

    /**
     * 总分享数
     */
    private Long totalShares;

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    private LocalDate date;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
