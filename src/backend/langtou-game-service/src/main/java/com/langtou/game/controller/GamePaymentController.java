package com.langtou.game.controller;

import com.langtou.common.result.Result;
import com.langtou.game.dto.GamePaymentCreateRequest;
import com.langtou.game.dto.GamePaymentResponse;
import com.langtou.game.service.GamePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game/payments")
@RequiredArgsConstructor
@Tag(name = "游戏支付", description = "订单创建、支付回调、退款")
@SecurityRequirement(name = "bearer-jwt")
public class GamePaymentController {

    private final GamePaymentService paymentService;

    @PostMapping("/orders")
    @Operation(summary = "创建订单")
    public Result<GamePaymentResponse> createOrder(@Valid @RequestBody GamePaymentCreateRequest request) {
        return Result.success(paymentService.createOrder(request));
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "查询订单")
    public Result<GamePaymentResponse> getOrder(@PathVariable String orderNo) {
        return Result.success(paymentService.getOrder(orderNo));
    }

    @PostMapping("/callback")
    @Operation(summary = "支付回调")
    public Result<Void> callback(@RequestParam String orderNo,
                                 @RequestParam String channelTransactionId,
                                 @RequestParam String status) {
        paymentService.handlePaymentCallback(orderNo, channelTransactionId, status);
        return Result.success();
    }

    @PostMapping("/orders/{orderNo}/refund")
    @Operation(summary = "退款")
    public Result<GamePaymentResponse> refund(@PathVariable String orderNo,
                                             @RequestParam(required = false) String reason) {
        return Result.success(paymentService.refund(orderNo, reason));
    }
}
