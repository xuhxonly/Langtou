package com.langtou.message.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.common.utils.PageUtils;
import com.langtou.message.entity.Notification;
import com.langtou.message.service.NotificationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public Result<PageUtils.PageResult<Notification>> getNotifications(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String type,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Page<Notification> page = notificationService.getNotifications(userId, current, size, type);
        return Result.success(PageUtils.PageResult.of(page));
    }

    @GetMapping("/notifications/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @PutMapping("/notifications/read-all")
    public Result<Void> markAllAsRead(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        notificationService.markAllAsRead(userId);
        return Result.success("全部已读成功");
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id,
                                    @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        notificationService.markAsRead(userId, id);
        return Result.success("标记已读成功");
    }
}
