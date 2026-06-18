package com.langtou.message.controller;

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
