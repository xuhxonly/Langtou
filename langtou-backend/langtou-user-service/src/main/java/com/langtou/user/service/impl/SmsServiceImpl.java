package com.langtou.user.service.impl;

import com.langtou.user.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 短信服务实现
 * 支持多模式：dev（开发模式，仅日志）、aliyun（阿里云短信SDK）
 *
 * 阿里云短信接入指南：
 * 1. 添加依赖 com.aliyun:dysmsapi20170525:3.0.0
 * 2. 配置 sms.aliyun.accessKeyId, sms.aliyun.accessKeySecret, sms.aliyun.signName, sms.aliyun.templateCode
 * 3. 设置 sms.enabled=true, sms.provider=aliyun
 */
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.provider:dev}")
    private String smsProvider;

    @Value("${sms.aliyun.accessKeyId:}")
    private String aliyunAccessKeyId;

    @Value("${sms.aliyun.accessKeySecret:}")
    private String aliyunAccessKeySecret;

    @Value("${sms.aliyun.signName:}")
    private String aliyunSignName;

    @Value("${sms.aliyun.templateCode:}")
    private String aliyunTemplateCode;

    @Override
    public void sendSmsCode(String phone, String code) {
        if (!smsEnabled || "dev".equals(smsProvider)) {
            log.info("[开发模式] 短信验证码已生成: phone={}, code已存入Redis", phone);
            // 开发模式：验证码已由 UserServiceImpl 存入 Redis，此处仅记录日志
            return;
        }

        if ("aliyun".equals(smsProvider)) {
            sendViaAliyun(phone, code);
        } else {
            log.warn("[生产模式] 短信服务商 '{}' 尚未实现，验证码仅存入Redis", smsProvider);
        }
    }

    /**
     * 通过阿里云短信 SDK 发送验证码
     * 使用 try/catch 包裹，SDK 类找不到时降级到日志模式
     */
    private void sendViaAliyun(String phone, String code) {
        try {
            // 动态加载阿里云 SDK 类，避免在 dev 模式下因缺少依赖而启动失败
            com.aliyun.dysmsapi20170525.Client client = createAliyunClient();
            com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(aliyunSignName)
                    .setTemplateCode(aliyunTemplateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            com.aliyun.dysmsapi20170525.models.SendSmsResponse response = client.sendSms(sendSmsRequest);

            if ("OK".equals(response.getBody().getCode())) {
                log.info("[阿里云短信] 发送成功: phone={}, bizId={}", phone, response.getBody().getBizId());
            } else {
                log.error("[阿里云短信] 发送失败: phone={}, code={}, message={}",
                        phone, response.getBody().getCode(), response.getBody().getMessage());
            }
        } catch (NoClassDefFoundError e) {
            log.warn("[阿里云短信] SDK 依赖未找到，降级到日志模式。请确认已添加 dysmsapi20170525 依赖: phone={}", phone);
            log.info("[降级模式] 短信验证码: phone={}, code={}", phone, code);
        } catch (Exception e) {
            log.error("[阿里云短信] 发送异常: phone={}, error={}", phone, e.getMessage(), e);
            // 发送失败时降级，验证码已存入 Redis 可用于测试
        }
    }

    /**
     * 创建阿里云短信客户端
     */
    private com.aliyun.dysmsapi20170525.Client createAliyunClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(aliyunAccessKeyId)
                .setAccessKeySecret(aliyunAccessKeySecret)
                .setEndpoint("dysmsapi.aliyuncs.com");
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    @Override
    public boolean isAvailable() {
        return smsEnabled && !"dev".equals(smsProvider);
    }
}
