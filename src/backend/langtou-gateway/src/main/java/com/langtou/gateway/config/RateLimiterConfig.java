package com.langtou.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown"
        );
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            ServerHttpRequest request = exchange.getRequest();
            String userId = request.getHeaders().getFirst("X-User-Id");
            if (userId == null || userId.isBlank()) {
                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        String token = authHeader.substring(7);
                        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parser()
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        userId = claims.get("userId", String.class);
                    } catch (Exception e) {
                        userId = null;
                    }
                }
            }
            return Mono.just(userId != null && !userId.isBlank() ? userId : "anonymous");
        };
    }
}
