package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备指纹实体
 */
@Data
@TableName("device_fingerprints")
public class DeviceFingerprint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID（未登录时为空）
     */
    private Long userId;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 设备品牌（如 Apple, Huawei）
     */
    private String deviceBrand;

    /**
     * 设备型号（如 iPhone 15, Mate 60）
     */
    private String deviceModel;

    /**
     * 操作系统类型: IOS/ANDROID/HARMONY
     */
    private String osType;

    /**
     * 操作系统版本
     */
    private String osVersion;

    /**
     * 应用版本号
     */
    private String appVersion;

    /**
     * IP地址（支持IPv6）
     */
    private String ipAddress;

    /**
     * 首次出现时间
     */
    private LocalDateTime firstSeenAt;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastSeenAt;

    /**
     * 是否封禁: 0-正常, 1-封禁
     */
    private Integer isBlocked;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
