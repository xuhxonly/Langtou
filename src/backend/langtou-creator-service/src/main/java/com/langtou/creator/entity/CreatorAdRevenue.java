package com.langtou.creator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("creator_ad_revenue")
public class CreatorAdRevenue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private Long noteId;

    /**
     * 广告类型：IMPRESSION-曝光 / CLICK-点击
     */
    private String adType;

    private Integer impressions;

    private Integer clicks;

    private BigDecimal ctr;

    private BigDecimal revenue;

    /**
     * 结算状态：UNSETTLED-未结算 / SETTLED-已结算
     */
    private String settlementStatus;

    private java.time.LocalDate settlementDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
