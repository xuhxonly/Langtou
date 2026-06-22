package com.langtou.ad.service;

import com.langtou.ad.entity.Advertisement;

import java.util.List;
import java.util.Map;

public interface AdService {

    List<Advertisement> getFeedAds(int count);

    Advertisement getSplashAd();

    void recordImpression(Long adId, Long userId, Long noteId, Integer position);

    void recordClick(Long adId, Long userId, Long noteId);
}
