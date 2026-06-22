package com.langtou.user.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.user.dto.BindPhoneDTO;
import com.langtou.user.dto.ChangePasswordDTO;
import com.langtou.user.dto.SendSmsCodeDTO;
import com.langtou.user.dto.SmsLoginDTO;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserProfileVO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户服务", description = "用户注册、登录、个人资料、账号管理等接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/auth/register")
    @Operation(summary = "用户注册", description = "通过用户名/邮箱/密码注册新账号")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = UserDTO.class)))
    })
    public Result<UserDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserDTO userDTO = userService.register(registerDTO);
        return Result.success("注册成功", userDTO);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "账号登录", description = "通过用户名/邮箱+密码登录，返回JWT Token")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("登录成功", data);
    }

    @PostMapping("/auth/send-sms-code")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送登录验证码")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsCodeDTO dto) {
        userService.sendSmsCode(dto.getPhone());
        return Result.success("验证码发送成功", null);
    }

    @PostMapping("/auth/sms-login")
    @Operation(summary = "短信验证码登录", description = "通过手机号+验证码登录")
    public Result<Map<String, Object>> smsLogin(@Valid @RequestBody SmsLoginDTO smsLoginDTO) {
        String token = userService.smsLogin(smsLoginDTO);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("登录成功", data);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "根据ID获取用户", description = "获取指定用户的基本信息")
    @Parameter(name = "userId", description = "用户ID", required = true)
    public Result<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO userDTO = userService.getUserById(userId);
        return Result.success(userDTO);
    }

    @GetMapping("/users/me")
    @Operation(summary = "获取当前登录用户", description = "通过请求头中的用户ID获取当前登录用户信息")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<UserDTO> getCurrentUser(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        UserDTO userDTO = userService.getCurrentUser(userId);
        return Result.success(userDTO);
    }

    @PutMapping("/users/me/profile")
    @Operation(summary = "更新当前用户资料", description = "更新当前登录用户的昵称、简介等资料")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<UserProfileVO> updateProfile(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                               @Valid @RequestBody UserDTO userDTO) {
        UserProfileVO profile = userService.updateProfile(userId, userDTO);
        return Result.success("资料更新成功", profile);
    }

    @GetMapping("/users/{userId}/profile")
    @Operation(summary = "获取用户公开资料", description = "获取用户公开资料（粉丝数、关注数、笔记数）")
    public Result<UserProfileVO> getUserProfile(
            @Parameter(name = "userId", description = "用户ID", required = true)
            @PathVariable Long userId) {
        UserProfileVO profile = userService.getUserProfile(userId);
        return Result.success(profile);
    }

    @PostMapping("/users/me/avatar")
    @Operation(summary = "上传头像", description = "上传用户头像，返回头像URL")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Map<String, String>> uploadAvatar(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                                    @RequestBody Map<String, String> body) {
        String fileUrl = body.get("avatarUrl");
        String avatarUrl = userService.uploadAvatar(userId, fileUrl);
        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        return Result.success("头像上传成功", data);
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "刷新Token", description = "使用当前登录会话刷新JWT Token")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Map<String, Object>> refreshToken(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        String token = userService.refreshToken(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("Token刷新成功", data);
    }

    @GetMapping("/users/batch")
    @Operation(summary = "批量获取用户", description = "根据用户ID列表批量获取用户信息")
    public Result<List<UserDTO>> batchGetUsers(
            @Parameter(name = "userIds", description = "用户ID列表", required = true)
            @RequestParam("userIds") List<Long> userIds) {
        List<UserDTO> users = userIds.stream()
                .map(userService::getUserById)
                .collect(Collectors.toList());
        return Result.success(users);
    }

    @GetMapping("/users/{userId}/following/ids")
    @Operation(summary = "获取关注用户ID列表", description = "获取指定用户关注的所有用户ID")
    public Result<List<Long>> getFollowingIds(
            @Parameter(name = "userId", description = "用户ID", required = true)
            @PathVariable Long userId) {
        List<Long> followingIds = userService.getFollowingIds(userId);
        return Result.success(followingIds);
    }

    @GetMapping("/users/search")
    @Operation(summary = "搜索用户", description = "按昵称或用户名模糊搜索用户")
    public Result<List<Map<String, Object>>> searchUsers(
            @Parameter(name = "keyword", description = "搜索关键词", required = true)
            @RequestParam String keyword,
            @Parameter(name = "limit", description = "返回数量限制")
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

    @PutMapping("/users/me/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的登录密码")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> changePassword(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                       @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(userId, changePasswordDTO);
        return Result.success("密码修改成功", null);
    }

    @PutMapping("/users/me/phone")
    @Operation(summary = "绑定/修改手机号", description = "绑定或修改当前登录用户的手机号")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> bindPhone(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                 @Valid @RequestBody BindPhoneDTO bindPhoneDTO) {
        userService.bindPhone(userId, bindPhoneDTO);
        return Result.success("手机号绑定成功", null);
    }
}