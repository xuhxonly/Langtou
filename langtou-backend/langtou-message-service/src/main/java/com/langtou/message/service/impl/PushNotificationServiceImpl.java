package com.langtou.message.service.impl;

import com.langtou.message.entity.PushDeviceToken;
import com.langtou.message.entity.PushLog;
import com.langtou.message.entity.PushSettings;
import com.langtou.message.mapper.PushDeviceTokenMapper;
import com.langtou.message.mapper.PushLogMapper;
import com.langtou.message.service.PushDeviceService;
import com.langtou.message.service.PushNotificationService;
import com.langtou.message.service.PushSettingsService;
import com.langtou.message.utils.PushFrequencyLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 推送通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushDeviceService pushDeviceService;
    private final PushSettingsService pushSettingsService;
    private final PushDeviceTokenMapper pushDeviceTokenMapper;
    private final PushLogMapper pushLogMapper;
    private final PushFrequencyLimiter pushFrequencyLimiter;
    private final ObjectMapper objectMapper;

    @Override
    public boolean sendPush(Long userId, String pushType, String title, String body, Map<String, String> data) {
        // 1. 获取用户推送设置
        PushSettings settings = pushSettingsService.getSettings(userId);

        // 2. 检查推送类型开关
        if (!isPushTypeEnabled(settings, pushType)) {
            log.info("推送被用户设置拦截: userId={}, pushType={}", userId, pushType);
            return false;
        }

        // 3. 检查免打扰时段（私信不受免打扰限制）
        if (!isPrivateMessage(pushType) && isInQuietHours(settings)) {
            log.info("推送被免打扰拦截: userId={}, pushType={}", userId, pushType);
            return false;
        }

        // 4. 检查每日频率限制（私信不受频率限制）
        if (!isPrivateMessage(pushType) && !pushFrequencyLimiter.tryAcquire(userId, settings.getDailyLimit())) {
            log.info("推送被频率限制拦截: userId={}, pushType={}", userId, pushType);
            return false;
        }

        // 5. 获取用户设备列表
        List<PushDeviceToken> devices = pushDeviceService.getUserDevices(userId);
        if (devices == null || devices.isEmpty()) {
            log.info("用户无注册设备，跳过推送: userId={}", userId);
            return false;
        }

        // 6. 逐设备发送推送
        boolean anySuccess = false;
        for (PushDeviceToken device : devices) {
            boolean success = sendToDevice(device, pushType, title, body, data);
            if (success) {
                anySuccess = true;
            }
        }

        return anySuccess;
    }

    @Override
    public int sendPushBatch(List<Long> userIds, String pushType, String title, String body, Map<String, String> data) {
        int successCount = 0;
        for (Long userId : userIds) {
            try {
                if (sendPush(userId, pushType, title, body, data)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("批量推送异常: userId={}, pushType={}, error={}", userId, pushType, e.getMessage());
            }
        }
        log.info("批量推送完成: totalCount={}, successCount={}, pushType={}", userIds.size(), successCount, pushType);
        return successCount;
    }

    /**
     * 向单个设备发送推送
     */
    private boolean sendToDevice(PushDeviceToken device, String pushType,
                                 String title, String body, Map<String, String> data) {
        PushLog pushLog = createPushLog(device.getUserId(), device.getDeviceToken(), pushType, title, body, data);

        try {
            pushLog.setStatus("PENDING");
            pushLogMapper.insert(pushLog);

            pushLog.setSentAt(LocalDateTime.now());

            // TODO: 根据设备类型调用FCM/APNs发送推送
            // TODO: FCM发送 - 调用Firebase Cloud Messaging HTTP v1 API
            // TODO: APNs发送 - 调用Apple Push Notification service HTTP/2 API
            if ("ANDROID".equalsIgnoreCase(device.getDeviceType())) {
                // TODO: 调用FCM推送
                // sendViaFCM(device.getDeviceToken(), title, body, data);
                log.info("FCM推送发送(预留): userId={}, deviceType={}, pushType={}",
                        device.getUserId(), device.getDeviceType(), pushType);
            } else if ("IOS".equalsIgnoreCase(device.getDeviceType())) {
                // TODO: 调用APNs推送
                // sendViaAPNs(device.getDeviceToken(), title, body, data);
                log.info("APNs推送发送(预留): userId={}, deviceType={}, pushType={}",
                        device.getUserId(), device.getDeviceType(), pushType);
            }

            pushLog.setStatus("SENT");
            pushLogMapper.updateById(pushLog);
            log.info("推送发送成功: userId={}, deviceType={}, pushType={}, title={}",
                    device.getUserId(), device.getDeviceType(), pushType, title);
            return true;

        } catch (Exception e) {
            pushLog.setStatus("FAILED");
            pushLog.setErrorMessage(e.getMessage());
            pushLogMapper.updateById(pushLog);
            log.error("推送发送失败: userId={}, deviceType={}, pushType={}, error={}",
                    device.getUserId(), device.getDeviceType(), pushType, e.getMessage());
            return false;
        }
    }

    /**
     * 创建推送日志记录
     */
    private PushLog createPushLog(Long userId, String deviceToken, String pushType,
                                   String title, String body, Map<String, String> data) {
        PushLog pushLog = new PushLog();
        pushLog.setUserId(userId);
        pushLog.setDeviceToken(deviceToken);
        pushLog.setPushType(pushType);
        pushLog.setTitle(title);
        pushLog.setBody(body);
        if (data != null && !data.isEmpty()) {
            try {
                pushLog.setData(objectMapper.writeValueAsString(data));
            } catch (JsonProcessingException e) {
                log.warn("推送数据序列化失败: {}", e.getMessage());
                pushLog.setData("{}");
            }
        }
        return pushLog;
    }

    /**
     * 检查推送类型是否在用户设置中启用
     */
    private boolean isPushTypeEnabled(PushSettings settings, String pushType) {
        return switch (pushType.toUpperCase()) {
            case "PRIVATE_MESSAGE" -> Boolean.TRUE.equals(settings.getPrivateMessageEnabled());
            case "INTERACTION" -> Boolean.TRUE.equals(settings.getInteractionEnabled());
            case "SYSTEM" -> Boolean.TRUE.equals(settings.getSystemEnabled());
            case "MARKETING" -> Boolean.TRUE.equals(settings.getMarketingEnabled());
            default -> true;
        };
    }

    /**
     * 判断是否为私信推送类型
     */
    private boolean isPrivateMessage(String pushType) {
        return "PRIVATE_MESSAGE".equalsIgnoreCase(pushType);
    }

    /**
     * 检查当前时间是否在免打扰时段内
     */
    private boolean isInQuietHours(PushSettings settings) {
        String startStr = settings.getQuietHoursStart();
        String endStr = settings.getQuietHoursEnd();
        if (startStr == null || endStr == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        LocalTime quietStart = LocalTime.parse(startStr);
        LocalTime quietEnd = LocalTime.parse(endStr);

        if (quietStart.isBefore(quietEnd)) {
            // 同一天内，如 09:00 ~ 17:00
            return !now.isBefore(quietStart) && now.isBefore(quietEnd);
        } else {
            // 跨越午夜，如 23:00 ~ 08:00
            return !now.isBefore(quietStart) || now.isBefore(quietEnd);
        }
    }
}
