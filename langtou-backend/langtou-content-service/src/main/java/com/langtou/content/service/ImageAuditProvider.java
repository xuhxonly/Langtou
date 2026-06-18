package com.langtou.content.service;

/**
 * 图片审核服务提供者接口
 * 支持切换不同审核服务商（阿里云、腾讯云、百度AI、网易易盾等）
 */
public interface ImageAuditProvider {

    /**
     * 审核单张图片
     *
     * @param imageUrl 图片URL
     * @return 审核结果：true通过，false不通过
     */
    boolean auditImage(String imageUrl);

    /**
     * 获取服务商名称
     */
    String getProviderName();

    /**
     * 检查服务商是否可用
     */
    boolean isAvailable();
}
