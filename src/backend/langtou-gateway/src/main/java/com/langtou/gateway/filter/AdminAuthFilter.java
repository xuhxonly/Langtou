package com.langtou.gateway.filter;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 管理员权限过滤器
 * 对 /api/v1/admin/** 路径进行权限校验
 * 在 JwtAuthFilter 之前执行，自行解析 JWT token 并校验 role
 */
@Slf4j
@Component
public class AdminAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> ADMIN_PATHS = List.of(
        "/api/v1/admin/users",
        "/api/v1/admin/notes",
        "/api/v1/admin/audit",
        "/api/v1/admin/sensitive-words",
        "/api/v1/admin/ads",
        "/api/v1/admin/analytics",
        "/api/v1/admin/settings",
        "/api/v1/admin/system"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 检查是否为管理员路径
        boolean isAdminPath = ADMIN_PATHS.stream().anyMatch(path::startsWith);
        if (!isAdminPath) {
            return chain.filter(exchange);
        }

        // 优先读取 JwtAuthFilter 已解析的 header（若其已执行）
        String userRole = request.getHeaders().getFirst(CommonConstants.REQUEST_USER_ROLE);
        if (!"ADMIN".equals(userRole)) {
            // 若 header 未设置，自行解析 JWT token
            String token = resolveToken(request);
            if (token != null && JwtUtils.validateToken(token)) {
                userRole = JwtUtils.getRole(token);
            }
        }

        if (!"ADMIN".equals(userRole)) {
            log.warn("非管理员访问管理接口: path={}, role={}", path, userRole);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    @Override
    public int getOrder() {
        return -101; // 在 JwtAuthFilter(-100) 之前执行
    }
}
