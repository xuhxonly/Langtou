package com.langtou.message.service;

import com.langtou.message.entity.PushDeviceToken;

import java.util.List;

/**
 * 推送设备管理服务
 */
public interface PushDeviceService {

    /**
     * 注册设备Token
     *
     * @param userId      用户ID
     * @param deviceType  设备类型 (ANDROID/IOS)
     * @param deviceToken 设备Token
     * @param appVersion  App版本号
     * @param osVersion   操作系统版本
     * @return 注册后的设备Token记录
     */
    PushDeviceToken registerDevice(Long userId, String deviceType, String deviceToken,
                                    String appVersion, String osVersion);

    /**
     * 注销设备Token
     *
     * @param userId      用户ID
     * @param deviceToken 设备Token
     */
    void unregisterDevice(Long userId, String deviceToken);

    /**
     * 获取用户的所有设备Token列表
     *
     * @param userId 用户ID
     * @return 设备Token列表
     */
    List<PushDeviceToken> getUserDevices(Long userId);
}
