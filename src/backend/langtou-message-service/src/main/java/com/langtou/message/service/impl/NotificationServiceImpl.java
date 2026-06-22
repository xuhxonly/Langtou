package com.langtou.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.message.entity.Notification;
import com.langtou.message.mapper.NotificationMapper;
import com.langtou.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public Page<Notification> getNotifications(Long userId, long current, long size, String type) {
        Page<Notification> page = new Page<>(current, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type.toUpperCase());
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectPage(page, wrapper);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND);
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该通知");
        }
        notification.setIsRead(1);
        notificationMapper.updateById(notification);
        log.info("通知标记已读: userId={}, notificationId={}", userId, notificationId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
        log.info("全部通知标记已读: userId={}", userId);
    }

    @Override
    public void save(Notification notification) {
        notificationMapper.insert(notification);
    }
}
