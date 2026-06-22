package com.langtou.message.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.result.Result;
import com.langtou.message.dto.NotificationCreateDTO;
import com.langtou.message.entity.Notification;
import com.langtou.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "内部通知服务", description = "通知内部接口")
    public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notifications/internal")
    public Result<Void> createNotification(@RequestBody NotificationCreateDTO dto) {
        Notification notification = new Notification();
        BeanUtils.copyProperties(dto, notification);
        notification.setIsRead(0);
        notificationService.save(notification);
        log.info("创建通知成功: userId={}, type={}, sourceId={}", dto.getUserId(), dto.getType(), dto.getSourceId());
        return Result.success();
    }
}
