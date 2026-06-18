package com.langtou.common.client;

import com.langtou.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "langtou-message-service", path = "/api/v1", fallbackFactory = NotificationClient.NotificationClientFallbackFactory.class)
public interface NotificationClient {

    @PostMapping("/notifications/internal")
    Result<Void> createNotification(@RequestBody Map<String, Object> notification);

    @Slf4j
    @Component
    class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {
        @Override
        public NotificationClient create(Throwable cause) {
            return new NotificationClient() {
                @Override
                public Result<Void> createNotification(Map<String, Object> notification) {
                    log.error("NotificationClient.createNotification 调用失败, notification={}", notification, cause);
                    return Result.error("通知服务不可用");
                }
            };
        }
    }
}
