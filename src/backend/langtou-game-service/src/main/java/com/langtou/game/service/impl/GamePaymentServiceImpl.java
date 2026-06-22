﻿﻿﻿﻿﻿package com.langtou.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GamePaymentCreateRequest;
import com.langtou.game.dto.GamePaymentResponse;
import com.langtou.game.enums.PaymentStatus;
import com.langtou.game.entity.GamePayment;
import com.langtou.game.mapper.GamePaymentMapper;
import com.langtou.game.service.GamePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamePaymentServiceImpl implements GamePaymentService {

    private final GamePaymentMapper paymentMapper;
    private final Random random = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GamePaymentResponse createOrder(GamePaymentCreateRequest request) {
        if (request.getUserId() == null) {
            throw new BusinessException("用户ID不能为空");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException("支付金额必须大于 0（单位：分）");
        }
        GamePayment payment = new GamePayment();
        payment.setUserId(request.getUserId());
        payment.setOrderNo(generateOrderNo());
        payment.setProductId(request.getProductId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency() == null ? "CNY" : request.getCurrency());
        payment.setChannel(request.getChannel() == null ? "IN_APP" : request.getChannel());
        payment.setStatus(PaymentStatus.PENDING.name());
        paymentMapper.insert(payment);
        return toResponse(payment);
    }

    @Override
    public GamePaymentResponse getOrder(String orderNo) {
        LambdaQueryWrapper<GamePayment> wrapper = new LambdaQueryWrapper<GamePayment>()
                .eq(GamePayment::getOrderNo, orderNo);
        GamePayment payment = paymentMapper.selectOne(wrapper);
        if (payment == null) {
            throw new BusinessException("订单不存在");
        }
        return toResponse(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String orderNo, String channelTransactionId, String status) {
        LambdaQueryWrapper<GamePayment> wrapper = new LambdaQueryWrapper<GamePayment>()
                .eq(GamePayment::getOrderNo, orderNo);
        GamePayment payment = paymentMapper.selectOne(wrapper);
        if (payment == null) {
            throw new BusinessException("订单不存在");
        }

        PaymentStatus current = resolveStatus(payment.getStatus());
        PaymentStatus target = resolveStatus(status);
        if (current == null || target == null) {
            throw new BusinessException("未知的支付状态");
        }

        boolean legal = (current == PaymentStatus.PENDING && (target == PaymentStatus.SUCCESS || target == PaymentStatus.FAILED));
        if (!legal) {
            throw new BusinessException("非法支付状态流转：" + current + " -> " + target);
        }

        payment.setStatus(target.name());
        if (target == PaymentStatus.SUCCESS) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentMapper.updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GamePaymentResponse refund(String orderNo, String reason) {
        LambdaQueryWrapper<GamePayment> wrapper = new LambdaQueryWrapper<GamePayment>()
                .eq(GamePayment::getOrderNo, orderNo);
        GamePayment payment = paymentMapper.selectOne(wrapper);
        if (payment == null) {
            throw new BusinessException("订单不存在");
        }

        PaymentStatus current = resolveStatus(payment.getStatus());
        if (current == PaymentStatus.REFUNDED) {
            return toResponse(payment);
        }
        if (current != PaymentStatus.SUCCESS) {
            throw new BusinessException("订单状态不允许退款");
        }
        payment.setStatus(PaymentStatus.REFUNDED.name());
        paymentMapper.updateById(payment);
        return toResponse(payment);
    }

    private GamePaymentResponse toResponse(GamePayment payment) {
        GamePaymentResponse response = new GamePaymentResponse();
        response.setId(payment.getId());
        response.setOrderNo(payment.getOrderNo());
        response.setProductId(payment.getProductId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setChannel(payment.getChannel());
        response.setStatus(payment.getStatus());
        response.setPaidAt(payment.getPaidAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    private PaymentStatus resolveStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String generateOrderNo() {
        long ts = System.currentTimeMillis();
        int rand = random.nextInt(10000);
        return "P" + ts + String.format("%04d", rand);
    }
}