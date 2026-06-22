package com.langtou.user.service;

public interface SmsService {
    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param code 验证码
     */
    void sendSmsCode(String phone, String code);

    /**
     * 检查短信服务是否可用
     * @return true if available
     */
    boolean isAvailable();
}
