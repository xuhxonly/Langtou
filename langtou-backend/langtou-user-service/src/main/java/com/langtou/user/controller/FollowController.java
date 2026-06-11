package com.langtou.user.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.user.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    /**
     * 关注用户
     */
    @PostMapping("/users/{userId}/follow")
    public Result<Void> follow(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId,
                               @PathVariable Long userId) {
        followService.follow(currentUserId, userId);
        return Result.success("关注成功");
    }

    /**
     * 取消关注
     */
    @DeleteMapping("/users/{userId}/follow")
    public Result<Void> unfollow(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId,
                                 @PathVariable Long userId) {
        followService.unfollow(currentUserId, userId);
        return Result.success("取消关注成功");
    }

    /**
     * 获取粉丝列表（分页）
     */
    @GetMapping("/users/{userId}/followers")
    public Result<PageResult<?>> getFollowers(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFollowers(userId, page, size));
    }

    /**
     * 获取关注列表（分页）
     */
    @GetMapping("/users/{userId}/following")
    public Result<PageResult<?>> getFollowing(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getFollowing(userId, page, size));
    }

    /**
     * 查询与当前用户的关系
     */
    @GetMapping("/users/{userId}/relationship")
    public Result<Map<String, Object>> getRelationship(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long currentUserId,
                                                       @PathVariable Long userId) {
        int relationship = followService.getRelationship(currentUserId, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("following", relationship == 1 || relationship == 2);
        data.put("mutual", relationship == 2);
        return Result.success(data);
    }
}
