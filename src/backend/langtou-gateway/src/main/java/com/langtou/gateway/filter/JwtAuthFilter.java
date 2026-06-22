package com.langtou.gateway.filter;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpMethod;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    /**
     * 白名单：仅放行必须公开的精确路径，避免过于宽泛的通配符。
     * 格式为 "METHOD:PATH"，不区分方法的路径使用 "GET:/api/v1/notes" 格式。
     */
    private static final List<String> WHITE_LIST = List.of(
            "POST:/api/v1/auth/login",
            "POST:/api/v1/auth/sms-login",
            "POST:/api/v1/auth/register",
            "POST:/api/v1/auth/send-sms-code",
            "POST:/api/v1/admin/auth/login",
            "GET:/api/v1/notes",
            "GET:/api/v1/tags/hot",
            "GET:/api/v1/users/search",
            "GET:/api/v1/search/notes",
            "GET:/api/v1/search/users"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (isWhiteList(method, path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(request);
        if (token == null) {
            log.warn("请求缺少Token, path={}", path);
            return unauthorized(exchange.getResponse(), "未授权，请先登录");
        }

        if (!JwtUtils.validateToken(token)) {
            log.warn("Token无效或已过期, path={}", path);
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        Long userId = JwtUtils.getUserId(token);
        String username = JwtUtils.getUsername(token);
        String role = JwtUtils.getRole(token);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CommonConstants.REQUEST_USER_ID, String.valueOf(userId))
                .header(CommonConstants.REQUEST_USERNAME, username)
                .header(CommonConstants.REQUEST_USER_ROLE, role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhiteList(HttpMethod method, String path) {
        String key = method.name() + ":" + path;
        return WHITE_LIST.contains(key);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(CommonConstants.TOKEN_HEADER);
        if (bearerToken != null && bearerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(CommonConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}", message, System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
