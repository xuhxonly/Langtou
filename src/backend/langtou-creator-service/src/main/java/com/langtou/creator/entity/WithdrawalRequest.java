package com.langtou.creator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("withdrawal_requests")
public class WithdrawalRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private BigDecimal amount;

    /**
     * 状态：PENDING-待审核 / APPROVED-已批准 / REJECTED-已拒绝 / COMPLETED-已完成
     */
    private String status;

    private String bankAccount;

    private String realName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    private String remark;
}
