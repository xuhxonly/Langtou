package com.langtou.common.interceptor;

import com.langtou.common.monitor.PerformanceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API性能监控拦截器
 * <p>
 * 记录每个API请求的处理时间，当P99延迟超过500ms时输出告警日志。
 * 记录请求路径、HTTP方法、处理时间、状态码。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiPerformanceInterceptor implements HandlerInterceptor {

    /**
     * P99告警阈值（毫秒），默认500ms
     */
    private static final long P99_ALERT_THRESHOLD_MS = 500;

    /**
     * 慢请求告警阈值（毫秒），超过此值的请求会输出WARN日志
     */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;

    private final PerformanceMetrics performanceMetrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 将请求开始时间存入请求属性中
        request.setAttribute("_apiStartTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("_apiStartTime");
        if (startTime == null) {
            return;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String path = request.getRequestURI();
        int status = response.getStatus();

        // 记录性能指标
        performanceMetrics.recordRequest(method, path, elapsedTime, status);

        // 慢请求告警
        if (elapsedTime >= SLOW_REQUEST_THRESHOLD_MS) {
            log.warn("[SLOW API] 方法: {} | 路径: {} | 耗时: {}ms | 状态码: {}",
                    method, path, elapsedTime, status);
        } else if (elapsedTime >= P99_ALERT_THRESHOLD_MS) {
            log.info("[API P99 ALERT] 方法: {} | 路径: {} | 耗时: {}ms | 状态码: {}",
                    method, path, elapsedTime, status);
        }
    }
}
