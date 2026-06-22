package com.langtou.ad.service;

import com.langtou.common.result.PageResult;
import com.langtou.ad.entity.Advertisement;

import java.util.Map;

public interface AdminAdService {

    PageResult<Advertisement> listAds(Integer page, Integer size, String adType, Integer status, String keyword);

    Advertisement createAd(Advertisement ad);

    Advertisement updateAd(Long adId, Advertisement ad);

    void deleteAd(Long adId);

    Advertisement updateAdStatus(Long adId, Integer status);

    Map<String, Object> getAdStats(Long adId);
}
