package com.langtou.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.Result;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理（管理员）", description = "管理员对用户的管理接口")
    public class AdminUserController {
    private final UserMapper userMapper;

    @GetMapping
    public Result<Page<User>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getNickname, keyword).or().like(User::getUsername, keyword).or().like(User::getPhone, keyword);
        }
        if ("disabled".equals(status)) {
            wrapper.eq(User::getStatus, 0);
        } else if ("active".equals(status)) {
            wrapper.eq(User::getStatus, 1);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return Result.success(userMapper.selectPage(pageParam, wrapper));
    }

    @GetMapping("/{userId}")
    public Result<User> getUserDetail(@PathVariable Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        user.setPasswordHash(null); // 不返回密码
        return Result.success(user);
    }

    @PostMapping("/{userId}/ban")
    public Result<Void> banUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        user.setStatus(0);
        userMapper.updateById(user);
        log.info("管理员封禁用户: userId={}", userId);
        return Result.success("封禁成功");
    }

    @PostMapping("/{userId}/unban")
    public Result<Void> unbanUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        user.setStatus(1);
        userMapper.updateById(user);
        log.info("管理员解封用户: userId={}", userId);
        return Result.success("解封成功");
    }
}
