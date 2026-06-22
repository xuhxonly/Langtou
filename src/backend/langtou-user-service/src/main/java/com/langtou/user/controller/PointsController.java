package com.langtou.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.user.dto.UserLevelDTO;
import com.langtou.user.entity.PointsRecord;
import com.langtou.user.mapper.PointsRecordMapper;
import com.langtou.user.service.UserLevelService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 积分控制器
 * 提供积分余额查询、积分记录分页、每日签到等接口
 */
@RestController
@RequestMapping("/api/v1/users/me/points")
@RequiredArgsConstructor
@Tag(name = "积分服务", description = "用户积分、等级相关接口")
    public class PointsController {

    private final UserLevelService userLevelService;
    private final PointsRecordMapper pointsRecordMapper;

    /**
     * 查询积分余额
     * GET /api/v1/users/me/points
     */
    @GetMapping
    public Result<Map<String, Object>> getPointsBalance(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        UserLevelDTO levelDTO = userLevelService.getUserLevel(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("points", levelDTO.getPoints());
        data.put("totalPoints", levelDTO.getTotalPoints());
        data.put("level", levelDTO.getLevel());
        data.put("experience", levelDTO.getExperience());
        data.put("nextLevelExperience", levelDTO.getNextLevelExperience());
        return Result.success(data);
    }

    /**
     * 积分记录（分页）
     * GET /api/v1/users/me/points/records
     */
    @GetMapping("/records")
    public Result<PageResult<PointsRecord>> getPointsRecords(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PointsRecord> pageParam = new Page<>(page, size);
        QueryWrapper<PointsRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("created_at");

        Page<PointsRecord> resultPage = pointsRecordMapper.selectPage(pageParam, wrapper);
        return Result.success(PageResult.of(resultPage));
    }

    /**
     * 每日签到
     * POST /api/v1/users/me/points/checkin
     */
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkIn(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        int points = userLevelService.checkIn(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("points", points);
        data.put("alreadyCheckedIn", points == 0);

        if (points > 0) {
            return Result.success("签到成功，获得" + points + "积分", data);
        } else {
            return Result.success("今日已签到", data);
        }
    }
}
