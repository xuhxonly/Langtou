package com.langtou.ad.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.langtou.ad.entity.AdClick;
import com.langtou.ad.entity.AdImpression;
import com.langtou.ad.entity.Advertisement;
import com.langtou.ad.mapper.AdClickMapper;
import com.langtou.ad.mapper.AdImpressionMapper;
import com.langtou.ad.mapper.AdvertisementMapper;
import com.langtou.ad.service.AdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdServiceImpl implements AdService {

    private final AdvertisementMapper advertisementMapper;
    private final AdImpressionMapper adImpressionMapper;
    private final AdClickMapper adClickMapper;

    @Override
    public List<Advertisement> getFeedAds(int count) {
        QueryWrapper<Advertisement> wrapper = new QueryWrapper<>();
        wrapper.eq("ad_type", "feed")
                .eq("status", 1)
                .le("start_time", LocalDateTime.now())
                .ge("end_time", LocalDateTime.now())
                .orderByDesc("position");

        List<Advertisement> ads = advertisementMapper.selectList(wrapper);

        if (ads.isEmpty()) {
            return Collections.emptyList();
        }

        return selectByWeight(ads, count);
    }

    @Override
    public Advertisement getSplashAd() {
        QueryWrapper<Advertisement> wrapper = new QueryWrapper<>();
        wrapper.eq("ad_type", "splash")
                .eq("status", 1)
                .le("start_time", LocalDateTime.now())
                .ge("end_time", LocalDateTime.now())
                .orderByDesc("position")
                .last("LIMIT 1");

        return advertisementMapper.selectOne(wrapper);
    }

    @Override
    public void recordImpression(Long adId, Long userId, Long noteId, Integer position) {
        try {
            AdImpression impression = new AdImpression();
            impression.setAdId(adId);
            impression.setUserId(userId != null ? userId : 0L);
            impression.setNoteId(noteId != null ? noteId : 0L);
            impression.setPosition(position != null ? position : 0);
            adImpressionMapper.insert(impression);

            advertisementMapper.incrementImpressions(adId);
        } catch (Exception e) {
            log.warn("记录广告曝光失败: adId={}, error={}", adId, e.getMessage());
        }
    }

    @Override
    public void recordClick(Long adId, Long userId, Long noteId) {
        try {
            AdClick click = new AdClick();
            click.setAdId(adId);
            click.setUserId(userId != null ? userId : 0L);
            click.setNoteId(noteId != null ? noteId : 0L);
            adClickMapper.insert(click);

            advertisementMapper.incrementClicks(adId);
        } catch (Exception e) {
            log.warn("记录广告点击失败: adId={}, error={}", adId, e.getMessage());
        }
    }

    private List<Advertisement> selectByWeight(List<Advertisement> ads, int count) {
        if (ads.size() <= count) {
            return ads;
        }

        int totalWeight = ads.stream()
                .mapToInt(ad -> Math.max(ad.getPosition(), 1))
                .sum();

        List<Advertisement> selected = new ArrayList<>();
        List<Advertisement> remaining = new ArrayList<>(ads);

        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            int random = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulativeWeight = 0;

            for (int j = 0; j < remaining.size(); j++) {
                int weight = Math.max(remaining.get(j).getPosition(), 1);
                cumulativeWeight += weight;
                if (random < cumulativeWeight) {
                    selected.add(remaining.get(j));
                    totalWeight -= weight;
                    remaining.remove(j);
                    break;
                }
            }
        }

        return selected;
    }
}
