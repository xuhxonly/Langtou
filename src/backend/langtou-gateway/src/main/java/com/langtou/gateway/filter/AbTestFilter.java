package com.langtou.gateway.filter;

import com.langtou.common.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A/B测试过滤器
 * 根据userId哈希分配实验组/对照组，支持多实验并行
 */
@Slf4j
@Component
public class AbTestFilter implements GlobalFilter, Ordered {

    /**
     * 实验配置列表
     * 每个实验包含：实验名、对照组比例(0-1)、实验组比例(0-1)
     */
    private static final List<ExperimentConfig> EXPERIMENTS = Arrays.asList(
            new ExperimentConfig("rank_model", 0.5, 0.5),   // 排序模型实验
            new ExperimentConfig("ui_style", 0.5, 0.5),     // UI样式实验
            new ExperimentConfig("recall_strategy", 0.5, 0.5) // 召回策略实验
    );

    private static final String HEADER_AB_TEST_PREFIX = "X-AB-Test-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String userIdStr = request.getHeaders().getFirst(CommonConstants.REQUEST_USER_ID);

        if (userIdStr == null || userIdStr.isEmpty()) {
            return chain.filter(exchange);
        }

        long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return chain.filter(exchange);
        }

        // 为每个实验计算分组
        ServerHttpRequest.Builder requestBuilder = request.mutate();
        for (ExperimentConfig exp : EXPERIMENTS) {
            String group = assignGroup(userId, exp.getName(), exp);
            requestBuilder.header(HEADER_AB_TEST_PREFIX + exp.getName(), group);
            log.debug("A/B测试分组: userId={}, experiment={}, group={}", userId, exp.getName(), group);
        }

        ServerHttpRequest mutatedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 根据userId和实验名分配实验组
     * 使用一致性哈希确保同一用户在同一实验中始终分配到同一组
     */
    private String assignGroup(long userId, String experimentName, ExperimentConfig config) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = experimentName + ":" + userId;
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            // 取前4字节转为整数 (0 ~ 2^32-1)
            long hashValue = ((hash[0] & 0xFFL) << 24)
                    | ((hash[1] & 0xFFL) << 16)
                    | ((hash[2] & 0xFFL) << 8)
                    | (hash[3] & 0xFFL);

            double ratio = hashValue / (double) (1L << 32);

            if (ratio < config.getControlRatio()) {
                return "control";
            } else if (ratio < config.getControlRatio() + config.getTreatmentRatio()) {
                return "treatment";
            } else {
                return "none";
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            return "control";
        }
    }

    @Override
    public int getOrder() {
        // 在JwtAuthFilter之后执行，确保userId已注入
        return -90;
    }

    /**
     * 实验配置
     */
    public static class ExperimentConfig {
        private final String name;
        private final double controlRatio;
        private final double treatmentRatio;

        public ExperimentConfig(String name, double controlRatio, double treatmentRatio) {
            this.name = name;
            this.controlRatio = controlRatio;
            this.treatmentRatio = treatmentRatio;
        }

        public String getName() {
            return name;
        }

        public double getControlRatio() {
            return controlRatio;
        }

        public double getTreatmentRatio() {
            return treatmentRatio;
        }
    }
}
