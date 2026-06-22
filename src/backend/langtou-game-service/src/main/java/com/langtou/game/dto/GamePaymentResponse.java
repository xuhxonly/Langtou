﻿﻿﻿﻿﻿package com.langtou.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GamePaymentResponse {

    private Long id;

    private String orderNo;

    private String productId;

    private Long amount;

    private String currency;

    private String channel;

    private String status;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
}