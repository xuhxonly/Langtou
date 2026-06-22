package com.langtou.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册设备Token请求DTO
 */
@Data
public class DeviceTokenRegisterDTO {

    /** 设备类型: ANDROID / IOS */
    @NotBlank(message = "设备类型不能为空")
    private String deviceType;

    /** FCM/APNs Device Token */
    @NotBlank(message = "设备Token不能为空")
    private String deviceToken;

    /** App版本号 */
    private String appVersion;

    /** 操作系统版本 */
    private String osVersion;
}
