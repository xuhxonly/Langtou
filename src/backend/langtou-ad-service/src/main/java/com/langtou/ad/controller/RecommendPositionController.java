package com.langtou.ad.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.annotation.RequireRole;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.ad.entity.RecommendPosition;
import com.langtou.ad.service.RecommendPositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/recommend-positions")
@RequiredArgsConstructor
@Tag(name = "推荐位服务", description = "推荐位接口")
    public class RecommendPositionController {

    private final RecommendPositionService recommendPositionService;

    @GetMapping("/{type}")
    public Result<List<RecommendPosition>> getActivePositions(@PathVariable String type) {
        List<RecommendPosition> positions = recommendPositionService.getActivePositionsByType(type);
        return Result.success(positions);
    }

    @PostMapping("/admin")
    @RequireRole("ADMIN")
    public Result<RecommendPosition> createPosition(@RequestBody RecommendPosition position) {
        RecommendPosition created = recommendPositionService.createPosition(position);
        return Result.success(created);
    }

    @PutMapping("/admin/{id}")
    @RequireRole("ADMIN")
    public Result<RecommendPosition> updatePosition(@PathVariable Long id, @RequestBody RecommendPosition position) {
        RecommendPosition updated = recommendPositionService.updatePosition(id, position);
        return Result.success(updated);
    }

    @DeleteMapping("/admin/{id}")
    @RequireRole("ADMIN")
    public Result<Void> deletePosition(@PathVariable Long id) {
        recommendPositionService.deletePosition(id);
        return Result.success("删除成功");
    }

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
