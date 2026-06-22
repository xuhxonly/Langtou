﻿﻿﻿﻿﻿package com.langtou.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GamePaymentCreateRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String productId;

    @NotNull
    private Long amount;

    private String currency;

    private String channel;
}