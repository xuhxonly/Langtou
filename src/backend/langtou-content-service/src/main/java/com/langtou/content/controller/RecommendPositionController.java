package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.entity.RecommendPosition;
import com.langtou.content.service.RecommendPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐位管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/recommend-positions")
@RequiredArgsConstructor
@Tag(name = "推荐位服务", description = "推荐位接口")
    public class RecommendPositionController {

    private final RecommendPositionService recommendPositionService;

    // ========== 用户端接口 ==========

    /**
     * 按位置类型获取活跃推荐位
     * GET /api/v1/recommend-positions/{type}
     */
    @GetMapping("/{type}")
    public Result<List<RecommendPosition>> getActivePositions(@PathVariable String type) {
        List<RecommendPosition> positions = recommendPositionService.getActivePositionsByType(type);
        return Result.success(positions);
    }

    // ========== 管理员接口 ==========

    /**
     * 创建推荐位
     * POST /api/v1/recommend-positions/admin
     */
    @PostMapping("/admin")
    @RequireRole("ADMIN")
    public Result<RecommendPosition> createPosition(@RequestBody RecommendPosition position) {
        RecommendPosition created = recommendPositionService.createPosition(position);
        return Result.success(created);
    }

    /**
     * 更新推荐位
     * PUT /api/v1/recommend-positions/admin/{id}
     */
    @PutMapping("/admin/{id}")
    @RequireRole("ADMIN")
    public Result<RecommendPosition> updatePosition(@PathVariable Long id, @RequestBody RecommendPosition position) {
        RecommendPosition updated = recommendPositionService.updatePosition(id, position);
        return Result.success(updated);
    }

    /**
     * 删除推荐位
     * DELETE /api/v1/recommend-positions/admin/{id}
     */
    @DeleteMapping("/admin/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deletePosition(@PathVariable Long id) {
        recommendPositionService.deletePosition(id);
        return Result.success("删除成功");
    }

    /**
     * 推荐位列表（管理端）
     * GET /api/v1/recommend-positions/admin
     */
    @GetMapping("/admin")
    @RequireRole("ADMIN")
    public Result<PageResult<RecommendPosition>> listPositions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String positionType,
            @RequestParam(required = false) String status) {
        PageResult<RecommendPosition> result = recommendPositionService.listPositions(page, size, positionType, status);
        return Result.success(result);
    }
}
