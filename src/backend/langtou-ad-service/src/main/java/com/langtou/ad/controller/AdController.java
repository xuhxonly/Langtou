package com.langtou.ad.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.ad.entity.Advertisement;
import com.langtou.ad.service.AdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "广告服务", description = "广告对外接口")
    public class AdController {

    private final AdService adService;

    @GetMapping("/feed")
    @RequireRole
    public Result<List<Advertisement>> getFeedAds(@RequestParam(defaultValue = "1") int count) {
        List<Advertisement> ads = adService.getFeedAds(count);
        return Result.success(ads);
    }

    @GetMapping("/splash")
    @RequireRole
    public Result<Advertisement> getSplashAd() {
        Advertisement ad = adService.getSplashAd();
        return Result.success(ad);
    }

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
