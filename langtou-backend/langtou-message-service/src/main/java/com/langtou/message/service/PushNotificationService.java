package com.langtou.message.service;

import java.util.List;
import java.util.Map;

/**
 * 推送通知服务
 */
public interface PushNotificationService {

    /**
     * 发送单条推送
     *
     * @param userId   目标用户ID
     * @param pushType 推送类型 (PRIVATE_MESSAGE/INTERACTION/SYSTEM/MARKETING)
     * @param title    推送标题
     * @param body     推送内容
     * @param data     附加数据
     * @return 是否发送成功
     */
    boolean sendPush(Long userId, String pushType, String title, String body, Map<String, String> data);

    /**
     * 批量发送推送
     *
     * @param userIds  目标用户ID列表
     * @param pushType 推送类型
     * @param title    推送标题
     * @param body     推送内容
     * @param data     附加数据
     * @return 成功发送的数量
     */
    int sendPushBatch(List<Long> userIds, String pushType, String title, String body, Map<String, String> data);
}
