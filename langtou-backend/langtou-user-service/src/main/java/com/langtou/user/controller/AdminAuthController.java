package com.langtou.user.controller;

import com.langtou.common.result.Result;
import com.langtou.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台认证接口
 * 注意：生产环境应使用独立的管理后台认证体系
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> adminLogin(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // TODO: 生产环境应使用独立的管理员账号体系，而非普通用户登录
        // 当前为 MVP 阶段，使用普通用户登录 + 角色判断
        try {
            var loginResult = userService.login(username, password);
            if (loginResult != null) {
                // TODO: 检查用户是否为管理员角色
                return Result.success(loginResult);
            }
        } catch (Exception e) {
            log.warn("管理员登录失败: username={}", username);
        }
        return Result.error("用户名或密码错误");
    }
}
