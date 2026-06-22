package com.langtou.creator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("creator_wallet")
public class CreatorWallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private BigDecimal totalRevenue;

    private BigDecimal availableBalance;

    private BigDecimal pendingAmount;

    private BigDecimal withdrawnAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
