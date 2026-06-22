package com.langtou.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送设备Token实体
 */
@Data
@TableName("push_device_tokens")
public class PushDeviceToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 设备类型: ANDROID / IOS */
    private String deviceType;

    /** FCM/APNs Device Token */
    private String deviceToken;

    /** App版本号 */
    private String appVersion;

    /** 操作系统版本 */
    private String osVersion;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
