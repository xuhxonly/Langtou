package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Advertisement;
import com.langtou.content.service.AdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告用户接口
 *
 * 权限说明：
 * - GET /api/v1/ads/feed, GET /api/v1/ads/splash - 所有登录用户可访问
 * - POST /api/v1/ads/{adId}/click, POST /api/v1/ads/{adId}/impression - 所有登录用户可访问
 */
@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "内容-广告", description = "内容中的广告相关接口")
    public class AdController {

    private final AdService adService;

    /**
     * 获取信息流广告（按权重随机）
     * 需要登录
     */
    @GetMapping("/feed")
    @RequireRole
    public Result<List<Advertisement>> getFeedAds(@RequestParam(defaultValue = "1") int count) {
        List<Advertisement> ads = adService.getFeedAds(count);
        return Result.success(ads);
    }

    /**
     * 获取开屏广告
     * 需要登录
     */
    @GetMapping("/splash")
    @RequireRole
    public Result<Advertisement> getSplashAd() {
        Advertisement ad = adService.getSplashAd();
        return Result.success(ad);
    }

    /**
     * 记录广告曝光
     * 需要登录
     */
    @PostMapping("/{adId}/impression")
    @RequireRole
    public Result<Void> recordImpression(
            @PathVariable Long adId,
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long noteId = body != null && body.get("noteId") != null
                ? Long.valueOf(body.get("noteId").toString()) : 0L;
        Integer position = body != null && body.get("position") != null
                ? Integer.valueOf(body.get("position").toString()) : 0;
        adService.recordImpression(adId, userId, noteId, position);
        return Result.success();
    }

    /**
     * 记录广告点击
     * 需要登录
     */
    @PostMapping("/{adId}/click")
    @RequireRole
    public Result<Void> recordClick(
            @PathVariable Long adId,
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long noteId = body != null && body.get("noteId") != null
                ? Long.valueOf(body.get("noteId").toString()) : 0L;
        adService.recordClick(adId, userId, noteId);
        return Result.success();
    }
}
