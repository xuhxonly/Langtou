package com.langtou.user.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.user.dto.SendSmsCodeDTO;
import com.langtou.user.dto.SmsLoginDTO;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserProfileVO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    public Result<UserDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserDTO userDTO = userService.register(registerDTO);
        return Result.success("注册成功", userDTO);
    }

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("登录成功", data);
    }

    /**
     * 发送短信验证码
     */
    @PostMapping("/auth/send-sms-code")
    public Result<Map<String, String>> sendSmsCode(@Valid @RequestBody SendSmsCodeDTO dto) {
        String code = userService.sendSmsCode(dto.getPhone());
        Map<String, String> data = new HashMap<>();
        data.put("code", code);
        return Result.success("验证码发送成功", data);
    }

    /**
     * 手机号验证码登录
     */
    @PostMapping("/auth/sms-login")
    public Result<Map<String, Object>> smsLogin(@Valid @RequestBody SmsLoginDTO smsLoginDTO) {
        String token = userService.smsLogin(smsLoginDTO);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("登录成功", data);
    }

    @GetMapping("/users/{userId}")
    public Result<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO userDTO = userService.getUserById(userId);
        return Result.success(userDTO);
    }

    @GetMapping("/users/me")
    public Result<UserDTO> getCurrentUser(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        UserDTO userDTO = userService.getCurrentUser(userId);
        return Result.success(userDTO);
    }

    @PutMapping("/users/me/profile")
    public Result<UserProfileVO> updateProfile(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                               @RequestBody UserDTO userDTO) {
        UserProfileVO profile = userService.updateProfile(userId, userDTO);
        return Result.success("资料更新成功", profile);
    }

    /**
     * 获取用户公开资料（粉丝数、关注数、笔记数）
     */
    @GetMapping("/users/{userId}/profile")
    public Result<UserProfileVO> getUserProfile(@PathVariable Long userId) {
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    /**
     * 上传头像（返回头像URL）
     */
    @PostMapping("/users/me/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                                    @RequestBody Map<String, String> body) {
        String fileUrl = body.get("avatarUrl");
        String avatarUrl = userService.uploadAvatar(userId, fileUrl);
        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        return Result.success("头像上传成功", data);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/auth/refresh")
    public Result<Map<String, Object>> refreshToken(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String token = userService.refreshToken(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("Token刷新成功", data);
    }

    /**
     * 批量获取用户信息
     */
    @GetMapping("/users/batch")
    public Result<List<UserDTO>> batchGetUsers(@RequestParam("userIds") List<Long> userIds) {
        List<UserDTO> users = userIds.stream()
                .map(userService::getUserById)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    /**
     * 获取用户关注的所有用户ID
     */
    @GetMapping("/users/{userId}/following/ids")
    public Result<List<Long>> getFollowingIds(@PathVariable Long userId) {
        List<Long> followingIds = userService.getFollowingIds(userId);
        return Result.success(followingIds);
    }

    /**
     * 搜索用户（按昵称或用户名模糊匹配）
     */
    @GetMapping("/users/search")
    public Result<List<Map<String, Object>>> searchUsers(@RequestParam String keyword,
                                                          @RequestParam(defaultValue = "20") int limit) {
        List<UserDTO> users = userService.searchUsers(keyword, limit);
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("avatar", u.getAvatar());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }
}
