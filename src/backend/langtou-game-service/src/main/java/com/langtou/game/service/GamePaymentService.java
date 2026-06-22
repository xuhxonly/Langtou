package com.langtou.game.service;

import com.langtou.game.dto.GamePaymentCreateRequest;
import com.langtou.game.dto.GamePaymentResponse;

public interface GamePaymentService {

    GamePaymentResponse createOrder(GamePaymentCreateRequest request);

    GamePaymentResponse getOrder(String orderNo);

    void handlePaymentCallback(String orderNo, String channelTransactionId, String status);

    GamePaymentResponse refund(String orderNo, String reason);
}
