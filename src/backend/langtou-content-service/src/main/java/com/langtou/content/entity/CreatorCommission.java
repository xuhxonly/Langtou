package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创作者佣金记录实体
 */
@Data
@TableName("creator_commissions")
public class CreatorCommission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private Long productId;

    private Long noteId;

    private String orderNo;

    private Long buyerId;

    private BigDecimal amount;

    private BigDecimal commissionAmount;

    /**
     * 状态：PENDING-待确认 / SETTLED-已结算 / REFUNDED-已退款
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
