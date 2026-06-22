package com.langtou.game.service.impl;

import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GamePaymentCreateRequest;
import com.langtou.game.dto.GamePaymentResponse;
import com.langtou.game.entity.GamePayment;
import com.langtou.game.mapper.GamePaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GamePaymentServiceImpl 单元测试")
class GamePaymentServiceImplTest {

    @Mock
    private GamePaymentMapper paymentMapper;

    @InjectMocks
    private GamePaymentServiceImpl gamePaymentService;

    private GamePayment payment;

    @BeforeEach
    void setUp() {
        payment = new GamePayment();
        payment.setId(1L);
        payment.setUserId(100L);
        payment.setOrderNo("P17000000000001234");
        payment.setProductId("PROD_001");
        payment.setAmount(6000L);
        payment.setCurrency("CNY");
        payment.setChannel("IN_APP");
        payment.setStatus("PENDING");
    }

    @Test
    @DisplayName("createOrder 成功创建订单")
    void createOrder_success() {
        GamePaymentCreateRequest request = new GamePaymentCreateRequest();
        request.setUserId(100L);
        request.setProductId("PROD_001");
        request.setAmount(6000L);

        when(paymentMapper.insert(any(GamePayment.class))).thenAnswer(invocation -> {
            GamePayment p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });

        GamePaymentResponse response = gamePaymentService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderNo()).startsWith("P");
        assertThat(response.getProductId()).isEqualTo("PROD_001");
        assertThat(response.getAmount()).isEqualTo(6000L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getCurrency()).isEqualTo("CNY");
        assertThat(response.getChannel()).isEqualTo("IN_APP");
        verify(paymentMapper).insert(any(GamePayment.class));
    }

    @Test
    @DisplayName("createOrder 用户ID为空抛出异常")
    void createOrder_nullUserId() {
        GamePaymentCreateRequest request = new GamePaymentCreateRequest();
        request.setProductId("PROD_001");
        request.setAmount(6000L);

        assertThatThrownBy(() -> gamePaymentService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户ID不能为空");
    }

    @Test
    @DisplayName("createOrder 金额非法抛异常")
    void createOrder_invalidAmount() {
        GamePaymentCreateRequest request = new GamePaymentCreateRequest();
        request.setUserId(100L);
        request.setProductId("PROD_001");
        request.setAmount(0L);

        assertThatThrownBy(() -> gamePaymentService.createOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("支付金额必须大于 0");
    }

    @Test
    @DisplayName("handlePaymentCallback 状态机流转 PENDING->SUCCESS")
    void handlePaymentCallback_pendingToSuccess() {
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        gamePaymentService.handlePaymentCallback("P17000000000001234", "CH_TX_001", "SUCCESS");

        ArgumentCaptor<GamePayment> captor = ArgumentCaptor.forClass(GamePayment.class);
        verify(paymentMapper).updateById(captor.capture());
        GamePayment updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo("SUCCESS");
        assertThat(updated.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("handlePaymentCallback 状态机流转 PENDING->FAILED")
    void handlePaymentCallback_pendingToFailed() {
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        gamePaymentService.handlePaymentCallback("P17000000000001234", "CH_TX_001", "FAILED");

        ArgumentCaptor<GamePayment> captor = ArgumentCaptor.forClass(GamePayment.class);
        verify(paymentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("handlePaymentCallback 非法状态流转 SUCCESS->PENDING")
    void handlePaymentCallback_illegalFlow() {
        payment.setStatus("SUCCESS");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        assertThatThrownBy(() -> gamePaymentService.handlePaymentCallback("P17000000000001234", "CH_TX_001", "PENDING"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法支付状态流转");
        verify(paymentMapper, never()).updateById(any(GamePayment.class));
    }

    @Test
    @DisplayName("handlePaymentCallback 非法状态流转 REFUNDED->SUCCESS")
    void handlePaymentCallback_refundedToSuccess() {
        payment.setStatus("REFUNDED");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        assertThatThrownBy(() -> gamePaymentService.handlePaymentCallback("P17000000000001234", "CH_TX_001", "SUCCESS"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法支付状态流转");
    }

    @Test
    @DisplayName("handlePaymentCallback 未知状态抛异常")
    void handlePaymentCallback_unknownStatus() {
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        assertThatThrownBy(() -> gamePaymentService.handlePaymentCallback("P17000000000001234", "CH_TX_001", "UNKNOWN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未知的支付状态");
    }

    @Test
    @DisplayName("refund 幂等性 - 已退款直接返回")
    void refund_idempotent() {
        payment.setStatus("REFUNDED");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        GamePaymentResponse response = gamePaymentService.refund("P17000000000001234", "user cancel");

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        verify(paymentMapper, never()).updateById(any(GamePayment.class));
    }

    @Test
    @DisplayName("refund 只有 SUCCESS 状态才能退款")
    void refund_successStatusOnly() {
        payment.setStatus("SUCCESS");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        GamePaymentResponse response = gamePaymentService.refund("P17000000000001234", "user cancel");

        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        verify(paymentMapper).updateById(any(GamePayment.class));
    }

    @Test
    @DisplayName("refund PENDING 状态不可退款")
    void refund_pendingNotAllowed() {
        payment.setStatus("PENDING");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        assertThatThrownBy(() -> gamePaymentService.refund("P17000000000001234", "user cancel"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单状态不允许退款");
        verify(paymentMapper, never()).updateById(any(GamePayment.class));
    }

    @Test
    @DisplayName("refund FAILED 状态不可退款")
    void refund_failedNotAllowed() {
        payment.setStatus("FAILED");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        assertThatThrownBy(() -> gamePaymentService.refund("P17000000000001234", "user cancel"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("订单状态不允许退款");
    }
}
