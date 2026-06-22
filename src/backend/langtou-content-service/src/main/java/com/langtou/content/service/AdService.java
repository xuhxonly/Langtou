package com.langtou.content.service;

import com.langtou.content.entity.Advertisement;

import java.util.List;
import java.util.Map;

/**
 * 广告服务接口
 */
public interface AdService {

    /**
     * 获取信息流广告（按权重随机）
     *
     * @param count 需要的广告数量
     * @return 广告列表
     */
    List<Advertisement> getFeedAds(int count);

    /**
     * 获取开屏广告
     *
     * @return 开屏广告（可能为null）
     */
    Advertisement getSplashAd();

    /**
     * 记录广告曝光
     *
     * @param adId      广告ID
     * @param userId    用户ID（未登录传0）
     * @param noteId    关联笔记ID（信息流上下文）
     * @param position  展示位置
     */
    void recordImpression(Long adId, Long userId, Long noteId, Integer position);

    /**
     * 记录广告点击
     *
     * @param adId      广告ID
     * @param userId    用户ID（未登录传0）
     * @param noteId    关联笔记ID（信息流上下文）
     */
    void recordClick(Long adId, Long userId, Long noteId);
}
