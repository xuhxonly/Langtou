package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.Advertisement;

import java.util.Map;

/**
 * Admin 广告管理服务接口
 */
public interface AdminAdService {

    /**
     * 广告列表（分页、筛选）
     */
    PageResult<Advertisement> listAds(Integer page, Integer size, String adType, Integer status, String keyword);

    /**
     * 创建广告
     */
    Advertisement createAd(Advertisement ad);

    /**
     * 更新广告
     */
    Advertisement updateAd(Long adId, Advertisement ad);

    /**
     * 删除广告
     */
    void deleteAd(Long adId);

    /**
     * 修改广告状态（上线/暂停）
     */
    Advertisement updateAdStatus(Long adId, Integer status);

    /**
     * 广告数据统计
     */
    Map<String, Object> getAdStats(Long adId);
}
