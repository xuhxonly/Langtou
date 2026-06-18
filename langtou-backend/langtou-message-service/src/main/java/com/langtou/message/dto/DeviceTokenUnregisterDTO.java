package com.langtou.message.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注销设备Token请求DTO
 */
@Data
public class DeviceTokenUnregisterDTO {

    /** 要注销的设备Token */
    @NotBlank(message = "设备Token不能为空")
    private String deviceToken;
}
