﻿﻿﻿﻿﻿package com.langtou.game.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("game_payment")
public class GamePayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String orderNo;

    private String productId;

    /** 金额（单位：分） */
    private Long amount;

    private String currency;

    private String channel;

    private String status;

    private LocalDateTime paidAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}