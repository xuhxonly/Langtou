package com.langtou.common.monitor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.StringJoiner;

/**
 * MyBatis慢查询监控拦截器
 * <p>
 * 拦截所有SQL执行，记录执行时间超过阈值的慢查询。
 * 阈值可通过 application.yml 中 slow-query.threshold 配置。
 * </p>
 *
 * <pre>
 * application.yml 配置示例:
 * slow-query:
 *   enabled: true
 *   threshold: 500  # 毫秒
 *   log-level: WARN
 * </pre>
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
@ConditionalOnProperty(prefix = "slow-query", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SlowQueryMonitor implements Interceptor {

    /**
     * 慢查询阈值（毫秒），默认500ms
     */
    @Value("${slow-query.threshold:500}")
    private long slowQueryThreshold;

    /**
     * 慢查询日志级别，默认WARN
     */
    @Value("${slow-query.log-level:WARN}")
    private String logLevel;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = boundSql.getSql();
        String sqlId = mappedStatement.getId();

        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (elapsedTime >= slowQueryThreshold) {
                String paramStr = buildParameterString(boundSql, parameter);
                String message = String.format(
                        "[SLOW SQL] 执行时间: %dms (阈值: %dms) | SQL ID: %s | SQL: %s | 参数: %s",
                        elapsedTime, slowQueryThreshold, sqlId,
                        sql.replaceAll("\\s+", " ").trim(), paramStr
                );

                if ("ERROR".equalsIgnoreCase(logLevel)) {
                    log.error(message);
                } else if ("WARN".equalsIgnoreCase(logLevel)) {
                    log.warn(message);
                } else {
                    log.info(message);
                }
            }
        }
    }

    /**
     * 构建参数字符串，用于慢SQL日志输出
     */
    private String buildParameterString(BoundSql boundSql, Object parameterObject) {
        if (boundSql.getParameterMappings() == null || boundSql.getParameterMappings().isEmpty()) {
            if (parameterObject != null) {
                return parameterObject.toString();
            }
            return "无参数";
        }

        StringJoiner paramJoiner = new StringJoiner(", ");
        for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
            String propertyName = parameterMapping.getProperty();
            Object value;
            if (boundSql.hasAdditionalParameter(propertyName)) {
                value = boundSql.getAdditionalParameter(propertyName);
            } else if (parameterObject == null) {
                value = null;
            } else {
                value = parameterObject;
            }
            paramJoiner.add(propertyName + "=" + (value != null ? value.toString() : "null"));
        }
        return paramJoiner.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可通过MyBatis配置覆盖默认值
        if (properties != null) {
            String threshold = properties.getProperty("threshold");
            if (threshold != null) {
                this.slowQueryThreshold = Long.parseLong(threshold);
            }
        }
    }
}
