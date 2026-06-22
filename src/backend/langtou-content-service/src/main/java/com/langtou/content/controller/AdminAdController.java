package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.Advertisement;
import com.langtou.content.service.AdminAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 广告管理接口
 *
 * 所有接口需要 ADMIN 角色（纵深防御，与网关层 AdminAuthFilter 互补）
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ads")
@RequiredArgsConstructor
@RequireRole("ADMIN")
@Tag(name = "广告管理（管理员）", description = "管理员广告接口")
    public class AdminAdController {

    private final AdminAdService adminAdService;

    /**
     * 广告列表（分页、筛选）
     */
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

    /**
     * 创建广告
     */
    @PostMapping
    public Result<Advertisement> createAd(@RequestBody Advertisement ad) {
        Advertisement created = adminAdService.createAd(ad);
        return Result.success(created);
    }

    /**
     * 更新广告
     */
    @PutMapping("/{adId}")
    public Result<Advertisement> updateAd(@PathVariable Long adId, @RequestBody Advertisement ad) {
        Advertisement updated = adminAdService.updateAd(adId, ad);
        return Result.success(updated);
    }

    /**
     * 删除广告
     */
    @DeleteMapping("/{adId}")
    public Result<Void> deleteAd(@PathVariable Long adId) {
        adminAdService.deleteAd(adId);
        return Result.success("删除成功");
    }

    /**
     * 修改广告状态（上线/暂停）
     */
    @PutMapping("/{adId}/status")
    public Result<Advertisement> updateAdStatus(@PathVariable Long adId, @RequestBody Map<String, Integer> body) {
        Integer status = body != null ? body.get("status") : null;
        Advertisement updated = adminAdService.updateAdStatus(adId, status);
        return Result.success(updated);
    }

    /**
     * 广告数据统计
     */
    @GetMapping("/{adId}/stats")
    public Result<Map<String, Object>> getAdStats(@PathVariable Long adId) {
        Map<String, Object> stats = adminAdService.getAdStats(adId);
        return Result.success(stats);
    }
}
