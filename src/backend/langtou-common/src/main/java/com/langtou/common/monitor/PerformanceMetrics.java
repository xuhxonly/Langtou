package com.langtou.common.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * API性能指标统计
 * <p>
 * 记录API请求次数、响应时间分布、错误率，按接口路径分组统计。
 * 优先使用Micrometer（如果可用），否则使用自建计数器。
 * </p>
 */
@Slf4j
@Component
public class PerformanceMetrics {

    /**
     * 按接口路径分组的指标数据
     */
    private final Map<String, ApiMetric> metricsMap = new ConcurrentHashMap<>();

    /**
     * 全局总请求数
     */
    private final LongAdder totalRequests = new LongAdder();

    /**
     * 全局总错误数
     */
    private final LongAdder totalErrors = new LongAdder();

    /**
     * 全局总响应时间（用于计算平均响应时间）
     */
    private final LongAdder totalResponseTime = new LongAdder();

    /**
     * 全局最大响应时间
     */
    private final AtomicLong maxResponseTime = new AtomicLong(0);

    /**
     * Micrometer MeterRegistry（可选依赖）
     */
    @Autowired(required = false)
    private io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        if (meterRegistry != null) {
            log.info("[PerformanceMetrics] Micrometer可用，将同时使用Micrometer和自建计数器记录指标");
        } else {
            log.info("[PerformanceMetrics] Micrometer不可用，使用自建计数器记录指标");
        }
    }

    /**
     * 记录一次API请求
     *
     * @param httpMethod  HTTP方法 (GET, POST, PUT, DELETE等)
     * @param path        请求路径
     * @param elapsedTime 响应时间（毫秒）
     * @param statusCode  HTTP状态码
     */
    public void recordRequest(String httpMethod, String path, long elapsedTime, int statusCode) {
        String metricKey = httpMethod + ":" + path;

        // 更新全局指标
        totalRequests.increment();
        totalResponseTime.add(elapsedTime);
        updateMaxResponseTime(elapsedTime);

        if (statusCode >= 400) {
            totalErrors.increment();
        }

        // 更新按路径分组的指标
        ApiMetric metric = metricsMap.computeIfAbsent(metricKey, k -> new ApiMetric());
        metric.record(elapsedTime, statusCode);

        // 如果Micrometer可用，同时记录到Micrometer
        if (meterRegistry != null) {
            recordToMicrometer(httpMethod, path, elapsedTime, statusCode);
        }
    }

    /**
     * 获取指定路径的指标
     */
    public ApiMetric getMetric(String httpMethod, String path) {
        return metricsMap.get(httpMethod + ":" + path);
    }

    /**
     * 获取全局总请求数
     */
    public long getTotalRequests() {
        return totalRequests.sum();
    }

    /**
     * 获取全局总错误数
     */
    public long getTotalErrors() {
        return totalErrors.sum();
    }

    /**
     * 获取全局平均响应时间（毫秒）
     */
    public double getAvgResponseTime() {
        long total = totalRequests.sum();
        if (total == 0) {
            return 0;
        }
        return (double) totalResponseTime.sum() / total;
    }

    /**
     * 获取全局最大响应时间（毫秒）
     */
    public long getMaxResponseTime() {
        return maxResponseTime.get();
    }

    /**
     * 获取全局错误率
     */
    public double getErrorRate() {
        long total = totalRequests.sum();
        if (total == 0) {
            return 0;
        }
        return (double) totalErrors.sum() / total * 100;
    }

    /**
     * 获取所有路径的指标
     */
    public Map<String, ApiMetric> getAllMetrics() {
        return Map.copyOf(metricsMap);
    }

    /**
     * 重置所有指标（用于测试或定期清理）
     */
    public void reset() {
        metricsMap.clear();
        totalRequests.reset();
        totalErrors.reset();
        totalResponseTime.reset();
        maxResponseTime.set(0);
        log.info("[PerformanceMetrics] 指标已重置");
    }

    /**
     * 更新最大响应时间（CAS操作保证线程安全）
     */
    private void updateMaxResponseTime(long elapsedTime) {
        long current;
        do {
            current = maxResponseTime.get();
            if (elapsedTime <= current) {
                break;
            }
        } while (!maxResponseTime.compareAndSet(current, elapsedTime));
    }

    /**
     * 记录指标到Micrometer
     */
    private void recordToMicrometer(String httpMethod, String path, long elapsedTime, int statusCode) {
        try {
            // 记录请求计数
            meterRegistry.counter("api.requests.total",
                    "method", httpMethod,
                    "path", path,
                    "status", String.valueOf(statusCode)
            ).increment();

            // 记录响应时间
            meterRegistry.timer("api.response.time",
                    "method", httpMethod,
                    "path", path
            ).record(java.time.Duration.ofMillis(elapsedTime));

            // 记录错误
            if (statusCode >= 400) {
                meterRegistry.counter("api.errors.total",
                        "method", httpMethod,
                        "path", path,
                        "status", String.valueOf(statusCode)
                ).increment();
            }
        } catch (Exception e) {
            log.debug("[PerformanceMetrics] Micrometer记录失败: {}", e.getMessage());
        }
    }

    /**
     * 单个API接口的指标数据
     */
    public static class ApiMetric {
        /**
         * 请求总数
         */
        private final LongAdder requestCount = new LongAdder();

        /**
         * 错误数（状态码 >= 400）
         */
        private final LongAdder errorCount = new LongAdder();

        /**
         * 总响应时间（毫秒）
         */
        private final LongAdder totalResponseTime = new LongAdder();

        /**
         * 最大响应时间（毫秒）
         */
        private final AtomicLong maxResponseTime = new AtomicLong(0);

        /**
         * 最小响应时间（毫秒）
         */
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

        /**
         * P50近似值（使用简单的中位数估算）
         */
        private final AtomicLong p50 = new AtomicLong(0);

        /**
         * P99近似值
         */
        private final AtomicLong p99 = new AtomicLong(0);

        /**
         * 记录一次请求
         */
        public synchronized void record(long elapsedTime, int statusCode) {
            requestCount.increment();
            totalResponseTime.add(elapsedTime);
            updateMax(elapsedTime);
            updateMin(elapsedTime);
            updatePercentiles(elapsedTime);

            if (statusCode >= 400) {
                errorCount.increment();
            }
        }

        public long getRequestCount() {
            return requestCount.sum();
        }

        public long getErrorCount() {
            return errorCount.sum();
        }

        public double getAvgResponseTime() {
            long count = requestCount.sum();
            if (count == 0) return 0;
            return (double) totalResponseTime.sum() / count;
        }

        public long getMaxResponseTime() {
            return maxResponseTime.get();
        }

        public long getMinResponseTime() {
            long min = minResponseTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public long getP50() {
            return p50.get();
        }

        public long getP99() {
            return p99.get();
        }

        public double getErrorRate() {
            long count = requestCount.sum();
            if (count == 0) return 0;
            return (double) errorCount.sum() / count * 100;
        }

        private void updateMax(long elapsedTime) {
            long current;
            do {
                current = maxResponseTime.get();
                if (elapsedTime <= current) break;
            } while (!maxResponseTime.compareAndSet(current, elapsedTime));
        }

        private void updateMin(long elapsedTime) {
            long current;
            do {
                current = minResponseTime.get();
                if (elapsedTime >= current) break;
            } while (!minResponseTime.compareAndSet(current, elapsedTime));
        }

        /**
         * 简单的百分位数估算（使用指数移动平均近似）
         */
        private void updatePercentiles(long elapsedTime) {
            // P50使用简单EMA近似
            long currentP50 = p50.get();
            if (currentP50 == 0) {
                p50.set(elapsedTime);
                p99.set(elapsedTime);
            } else {
                // EMA alpha = 0.1
                double alpha = 0.1;
                long newP50 = (long) (alpha * elapsedTime + (1 - alpha) * currentP50);
                p50.set(newP50);

                // P99使用更大的alpha衰减
                double alpha99 = 0.02;
                long currentP99 = p99.get();
                long newP99 = (long) (alpha99 * elapsedTime + (1 - alpha99) * currentP99);
                p99.set(newP99);
            }
        }

        @Override
        public String toString() {
            return String.format(
                    "ApiMetric{count=%d, errors=%d, avg=%.1fms, min=%dms, max=%dms, p50=%dms, p99=%dms, errorRate=%.1f%%}",
                    getRequestCount(), getErrorCount(), getAvgResponseTime(),
                    getMinResponseTime(), getMaxResponseTime(), getP50(), getP99(), getErrorRate()
            );
        }
    }
}
