package com.langtou.creator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("creator_daily_stats")
public class CreatorDailyStats {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private Integer newFollowers;

    private Integer unfollowers;

    private Integer totalFollowers;

    private Integer contentCount;

    private Long totalViews;

    private Long totalLikes;

    private Long totalComments;

    private Long totalShares;

    private BigDecimal totalRevenue;

    private LocalDate date;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
