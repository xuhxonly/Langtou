package com.langtou.gateway.config;

import com.langtou.gateway.fallback.CircuitBreakerFallback;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 网关熔断器配置
 * 将Resilience4j熔断器与各路由绑定，配置降级处理
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CircuitBreakerConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CircuitBreakerFallback fallback;

    /**
     * 获取指定名称的熔断器
     */
    private CircuitBreaker getCircuitBreaker(String name) {
        return circuitBreakerRegistry.circuitBreaker(name, circuitBreakerRegistry.getDefaultConfig());
    }

    /**
     * 创建通用的熔断降级GatewayFilter
     * 根据路由ID匹配对应的熔断器和降级处理器
     */
    @Bean
    public org.springframework.cloud.gateway.filter.factory.CircuitBreakerGatewayFilterFactory circuitBreakerGatewayFilterFactory() {
        return new org.springframework.cloud.gateway.filter.factory.CircuitBreakerGatewayFilterFactory(
                circuitBreakerRegistry
        );
    }

    /**
     * 用户服务熔断降级Handler
     */
    public Mono<Void> userServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 用户服务熔断降级触发");
        return Mono.error(new org.springframework.cloud.gateway.support.ServiceUnavailableException(
                "用户服务暂时不可用"
        ));
    }

    /**
     * 内容服务熔断降级Handler
     */
    public Mono<Void> contentServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 内容服务熔断降级触发");
        return Mono.error(new org.springframework.cloud.gateway.support.ServiceUnavailableException(
                "内容服务暂时不可用"
        ));
    }

    /**
     * 互动服务熔断降级Handler
     */
    public Mono<Void> interactServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 互动服务熔断降级触发");
        return Mono.error(new org.springframework.cloud.gateway.support.ServiceUnavailableException(
                "互动服务暂时不可用"
        ));
    }

    /**
     * 消息服务熔断降级Handler
     */
    public Mono<Void> messageServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 消息服务熔断降级触发");
        return Mono.error(new org.springframework.cloud.gateway.support.ServiceUnavailableException(
                "消息服务暂时不可用"
        ));
    }

    /**
     * 搜索服务熔断降级Handler
     */
    public Mono<Void> searchServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 搜索服务熔断降级触发");
        return Mono.error(new org.springframework.cloud.gateway.support.ServiceUnavailableException(
                "搜索服务暂时不可用"
        ));
    }

    /**
     * 埋点服务熔断降级Handler（静默处理）
     */
    public Mono<Void> analyticsServiceFallbackHandler(Throwable throwable) {
        log.warn("[CircuitBreaker] 埋点服务熔断降级触发（静默丢弃）");
        return Mono.empty();
    }
}
