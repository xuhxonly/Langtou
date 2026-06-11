package com.langtou.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.message.entity.Notification;

public interface NotificationService {

    Page<Notification> getNotifications(Long userId, long current, long size, String type);

    Long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
