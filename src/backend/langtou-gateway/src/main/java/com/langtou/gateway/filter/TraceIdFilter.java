package com.langtou.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * TraceId传递过滤器
 * 确保链路追踪的traceId在请求头中传递
 */
@Slf4j
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);

        // 如果请求中没有traceId，则生成一个新的
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        final String finalTraceId = traceId;

        // 将traceId添加到请求头中传递给下游服务
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();

        // 将traceId放入exchange attributes中，便于日志记录
        exchange.getAttributes().put(TRACE_ID_HEADER, finalTraceId);

        log.debug("TraceId传递: traceId={}, path={}", finalTraceId, request.getURI().getPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    // 请求完成后可以在这里记录链路信息
                    log.debug("TraceId完成: traceId={}, signal={}", finalTraceId, signalType);
                });
    }

    @Override
    public int getOrder() {
        // 最高优先级，确保traceId最先被设置
        return -200;
    }
}
