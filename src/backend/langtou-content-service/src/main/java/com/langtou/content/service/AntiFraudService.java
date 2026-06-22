package com.langtou.content.service;

import com.langtou.content.entity.ContentSimilarity;
import com.langtou.content.entity.DeviceFingerprint;
import com.langtou.content.entity.FraudReport;

import java.util.List;
import java.util.Map;

/**
 * 反作弊服务接口
 *
 * 提供设备指纹管理、行为异常检测、内容去重检测、举报管理等功能。
 */
public interface AntiFraudService {

    /**
     * 记录设备指纹
     * 如果设备已存在则更新最后活跃时间，否则新建记录
     *
     * @param userId      用户ID（可为空，未登录用户）
     * @param deviceId    设备唯一标识
     * @param deviceBrand 设备品牌
     * @param deviceModel 设备型号
     * @param osType      操作系统类型
     * @param osVersion   操作系统版本
     * @param appVersion  应用版本号
     * @param ipAddress   IP地址
     * @return 设备指纹记录
     */
    DeviceFingerprint recordDeviceFingerprint(Long userId, String deviceId, String deviceBrand,
                                                 String deviceModel, String osType, String osVersion,
                                                 String appVersion, String ipAddress);

    /**
     * 行为异常检测
     * 使用Redis滑动窗口检测用户在短时间内是否频繁执行同类操作
     *
     * @param userId     用户ID
     * @param actionType  操作类型（如 LIKE, COMMENT, FOLLOW, PUBLISH）
     * @return true表示检测到异常行为，false表示正常
     */
    boolean checkBehaviorAnomaly(Long userId, String actionType);

    /**
     * 内容去重检测
     * 使用文本SimHash和图片感知哈希检测内容是否重复
     *
     * @param userId     用户ID
     * @param text       文本内容
     * @param imageUrls  图片URL列表
     * @return 相似度最高的匹配列表，空列表表示未检测到重复
     */
    List<ContentSimilarity> detectDuplicateContent(Long userId, String text, List<String> imageUrls);

    /**
     * 举报作弊
     *
     * @param userId      举报人用户ID
     * @param fraudType   作弊类型: LIKE_SPAM/COMMENT_SPAM/FOLLOW_SPAM/CONTENT_DUPLICATE/ACCOUNT_ANOMALY
     * @param description 举报描述
     * @param evidence    证据（JSON格式）
     * @return 举报记录
     */
    FraudReport reportFraud(Long userId, String fraudType, String description, Map<String, Object> evidence);

    /**
     * 封禁设备
     *
     * @param deviceId 设备唯一标识
     */
    void blockDevice(String deviceId);

    /**
     * 检查设备是否被封禁
     *
     * @param deviceId 设备唯一标识
     * @return true表示已封禁，false表示正常
     */
    boolean isDeviceBlocked(String deviceId);

    /**
     * 获取举报列表（分页）
     *
     * @param page   页码
     * @param size   每页数量
     * @param status 状态筛选（可为空）
     * @return 举报列表
     */
    List<FraudReport> getFraudReports(int page, int size, String status);

    /**
     * 处理举报
     *
     * @param reportId    举报ID
     * @param status      处理结果: CONFIRMED/DISMISSED
     * @param processorId 处理人ID
     */
    void processFraudReport(Long reportId, String status, Long processorId);

    /**
     * 获取设备列表（分页）
     *
     * @param page 页码
     * @param size 每页数量
     * @return 设备指纹列表
     */
    List<DeviceFingerprint> getDeviceList(int page, int size);

    /**
     * 获取反作弊统计数据
     *
     * @return 统计数据Map
     */
    Map<String, Object> getStatistics();
}
