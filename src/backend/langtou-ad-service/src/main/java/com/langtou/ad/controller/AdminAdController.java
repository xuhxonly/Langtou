package com.langtou.ad.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.ad.entity.Advertisement;
import com.langtou.ad.service.AdminAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ads")
@RequiredArgsConstructor
@RequireRole("ADMIN")
@Tag(name = "广告管理（管理员）", description = "管理员广告接口")
    public class AdminAdController {

    private final AdminAdService adminAdService;

    @GetMapping
    public Result<PageResult<Advertisement>> listAds(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String adType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        PageResult<Advertisement> result = adminAdService.listAds(page, size, adType, status, keyword);
        return Result.success(result);
    }

    @PostMapping
    public Result<Advertisement> createAd(@RequestBody Advertisement ad) {
        Advertisement created = adminAdService.createAd(ad);
        return Result.success(created);
    }

    @PutMapping("/{adId}")
    public Result<Advertisement> updateAd(@PathVariable Long adId, @RequestBody Advertisement ad) {
        Advertisement updated = adminAdService.updateAd(adId, ad);
        return Result.success(updated);
    }

    @DeleteMapping("/{adId}")
    public Result<Void> deleteAd(@PathVariable Long adId) {
        adminAdService.deleteAd(adId);
        return Result.success("删除成功");
    }

    @PutMapping("/{adId}/status")
    public Result<Advertisement> updateAdStatus(@PathVariable Long adId, @RequestBody Map<String, Integer> body) {
        Integer status = body != null ? body.get("status") : null;
        Advertisement updated = adminAdService.updateAdStatus(adId, status);
        return Result.success(updated);
    }

    @GetMapping("/{adId}/stats")
    public Result<Map<String, Object>> getAdStats(@PathVariable Long adId) {
        Map<String, Object> stats = adminAdService.getAdStats(adId);
        return Result.success(stats);
    }
}
