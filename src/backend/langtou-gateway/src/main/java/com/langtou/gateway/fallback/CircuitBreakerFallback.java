package com.langtou.gateway.fallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关熔断降级响应处理器
 * 为各微服务提供统一的降级响应，返回缓存数据或默认响应
 */
@Slf4j
@Component
public class CircuitBreakerFallback {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 用户服务降级响应
     */
    public Mono<Void> userServiceFallback(ServerWebExchange exchange, Throwable cause) {
        return handleFallback(exchange, cause, "user-service", buildUserFallbackResponse());
    }

    /**
     * 内容服务降级响应
     */
    public Mono<Void> contentServiceFallback(ServerWebExchange exchange, Throwable cause) {
        return handleFallback(exchange, cause, "content-service", buildContentFallbackResponse());
    }

    /**
     * 互动服务降级响应
     */
    public Mono<Void> interactServiceFallback(ServerWebExchange exchange, Throwable cause) {
        return handleFallback(exchange, cause, "interact-service", buildInteractFallbackResponse());
    }

    /**
     * 消息服务降级响应
     */
    public Mono<Void> messageServiceFallback(ServerWebExchange exchange, Throwable cause) {
        return handleFallback(exchange, cause, "message-service", buildMessageFallbackResponse());
    }

    /**
     * 搜索服务降级响应
     */
    public Mono<Void> searchServiceFallback(ServerWebExchange exchange, Throwable cause) {
        return handleFallback(exchange, cause, "search-service", buildSearchFallbackResponse());
    }

    /**
     * 埋点服务降级响应（静默丢弃，不影响用户体验）
     */
    public Mono<Void> analyticsServiceFallback(ServerWebExchange exchange, Throwable cause) {
        log.warn("[CircuitBreaker] 埋点服务降级，静默丢弃请求: {}", exchange.getRequest().getPath());
        // 埋点降级直接返回成功，不阻塞客户端
        return writeJsonResponse(exchange.getResponse(), HttpStatus.OK, Map.of(
                "code", 200,
                "message", "ok",
                "data", null,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 通用降级处理
     */
    private Mono<Void> handleFallback(ServerWebExchange exchange, Throwable cause,
                                       String serviceName, Map<String, Object> fallbackData) {
        String path = exchange.getRequest().getPath().value();
        String routeId = getRouteId(exchange);

        if (cause instanceof CallNotPermittedException) {
            log.warn("[CircuitBreaker] 熔断器打开，服务降级: service={}, route={}, path={}",
                    serviceName, routeId, path);
        } else {
            log.warn("[CircuitBreaker] 服务调用异常，触发降级: service={}, route={}, path={}, error={}",
                    serviceName, routeId, path, cause.getMessage());
        }

        return writeJsonResponse(exchange.getResponse(), HttpStatus.SERVICE_UNAVAILABLE, fallbackData);
    }

    /**
     * 写入JSON响应
     */
    private Mono<Void> writeJsonResponse(ServerHttpResponse response, HttpStatus status,
                                          Map<String, Object> data) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            String body = objectMapper.writeValueAsString(data);
            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("[CircuitBreaker] 序列化降级响应失败", e);
            return Mono.empty();
        }
    }

    /**
     * 获取当前路由ID
     */
    private String getRouteId(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    // ============ 各服务降级响应构建 ============

    /**
     * 用户服务降级：返回缓存/默认用户信息
     */
    private Map<String, Object> buildUserFallbackResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", "用户服务暂时不可用，请稍后重试");
        data.put("data", null);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * 内容服务降级：返回空列表（不影响用户浏览）
     */
    private Map<String, Object> buildContentFallbackResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", "内容服务暂时不可用，请稍后重试");
        data.put("data", Map.of(
                "records", java.util.Collections.emptyList(),
                "total", 0,
                "page", 1,
                "size", 20
        ));
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * 互动服务降级：返回操作失败提示
     */
    private Map<String, Object> buildInteractFallbackResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", "互动服务暂时不可用，请稍后重试");
        data.put("data", null);
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * 消息服务降级：返回空消息列表
     */
    private Map<String, Object> buildMessageFallbackResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", "消息服务暂时不可用，请稍后重试");
        data.put("data", Map.of(
                "records", java.util.Collections.emptyList(),
                "total", 0,
                "unreadCount", 0
        ));
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    /**
     * 搜索服务降级：返回空搜索结果
     */
    private Map<String, Object> buildSearchFallbackResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("code", 503);
        data.put("message", "搜索服务暂时不可用，请稍后重试");
        data.put("data", Map.of(
                "records", java.util.Collections.emptyList(),
                "total", 0,
                "page", 1,
                "size", 20
        ));
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }
}
