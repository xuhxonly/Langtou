package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.AdClick;
import com.langtou.content.entity.AdImpression;
import com.langtou.content.entity.Advertisement;
import com.langtou.content.mapper.AdClickMapper;
import com.langtou.content.mapper.AdImpressionMapper;
import com.langtou.content.mapper.AdvertisementMapper;
import com.langtou.content.service.AdminAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAdServiceImpl implements AdminAdService {

    private final AdvertisementMapper advertisementMapper;
    private final AdImpressionMapper adImpressionMapper;
    private final AdClickMapper adClickMapper;

    @Override
    public PageResult<Advertisement> listAds(Integer page, Integer size, String adType, Integer status, String keyword) {
        Page<Advertisement> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        QueryWrapper<Advertisement> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(adType)) {
            wrapper.eq("ad_type", adType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword).or().like("target_url", keyword));
        }
        wrapper.orderByDesc("created_at");

        Page<Advertisement> resultPage = advertisementMapper.selectPage(pageParam, wrapper);
        return PageResult.of(resultPage);
    }

    @Override
    public Advertisement createAd(Advertisement ad) {
        if (ad.getStatus() == null) {
            ad.setStatus(2); // 默认审核中
        }
        if (ad.getImpressions() == null) {
            ad.setImpressions(0);
        }
        if (ad.getClicks() == null) {
            ad.setClicks(0);
        }
        if (ad.getBudget() == null) {
            ad.setBudget(BigDecimal.ZERO);
        }
        advertisementMapper.insert(ad);
        return ad;
    }

    @Override
    public Advertisement updateAd(Long adId, Advertisement ad) {
        Advertisement existing = advertisementMapper.selectById(adId);
        if (existing == null) {
            throw new IllegalArgumentException("广告不存在: " + adId);
        }
        ad.setId(adId);
        // 保护统计字段不被覆盖
        ad.setImpressions(null);
        ad.setClicks(null);
        advertisementMapper.updateById(ad);
        return advertisementMapper.selectById(adId);
    }

    @Override
    public void deleteAd(Long adId) {
        Advertisement existing = advertisementMapper.selectById(adId);
        if (existing == null) {
            throw new IllegalArgumentException("广告不存在: " + adId);
        }
        advertisementMapper.deleteById(adId);
    }

    @Override
    public Advertisement updateAdStatus(Long adId, Integer status) {
        Advertisement existing = advertisementMapper.selectById(adId);
        if (existing == null) {
            throw new IllegalArgumentException("广告不存在: " + adId);
        }
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            throw new IllegalArgumentException("无效的状态值: " + status);
        }
        existing.setStatus(status);
        advertisementMapper.updateById(existing);
        return existing;
    }

    @Override
    public Map<String, Object> getAdStats(Long adId) {
        Advertisement ad = advertisementMapper.selectById(adId);
        if (ad == null) {
            throw new IllegalArgumentException("广告不存在: " + adId);
        }

        // 今日曝光/点击统计
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        Long todayImpressions = adImpressionMapper.selectCount(
                new QueryWrapper<AdImpression>()
                        .eq("ad_id", adId)
                        .ge("created_at", todayStart)
                        .lt("created_at", todayEnd)
        );
        Long todayClicks = adClickMapper.selectCount(
                new QueryWrapper<AdClick>()
                        .eq("ad_id", adId)
                        .ge("created_at", todayStart)
                        .lt("created_at", todayEnd)
        );

        // 总曝光/点击
        Long totalImpressions = adImpressionMapper.selectCount(
                new QueryWrapper<AdImpression>().eq("ad_id", adId)
        );
        Long totalClicks = adClickMapper.selectCount(
                new QueryWrapper<AdClick>().eq("ad_id", adId)
        );

        // CTR
        BigDecimal ctr = BigDecimal.ZERO;
        if (totalImpressions != null && totalImpressions > 0) {
            ctr = BigDecimal.valueOf(totalClicks)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalImpressions), 2, RoundingMode.HALF_UP);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("adId", adId);
        stats.put("title", ad.getTitle());
        stats.put("status", ad.getStatus());
        stats.put("budget", ad.getBudget());
        stats.put("todayImpressions", todayImpressions != null ? todayImpressions : 0L);
        stats.put("todayClicks", todayClicks != null ? todayClicks : 0L);
        stats.put("totalImpressions", totalImpressions != null ? totalImpressions : 0L);
        stats.put("totalClicks", totalClicks != null ? totalClicks : 0L);
        stats.put("ctr", ctr + "%");
        return stats;
    }
}
