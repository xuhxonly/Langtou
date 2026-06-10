package com.langtou.user.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.Result;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserDTO userDTO = userService.register(registerDTO);
        return Result.success("注册成功", userDTO);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        String token = userService.login(loginDTO);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", CommonConstants.TOKEN_PREFIX.trim());
        return Result.success("登录成功", data);
    }

    @GetMapping("/info/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        return Result.success(userDTO);
    }

    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        UserDTO userDTO = userService.getCurrentUser(userId);
        return Result.success(userDTO);
    }

    @PutMapping("/me")
    public Result<UserDTO> updateUser(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                      @RequestBody UserDTO userDTO) {
        UserDTO updated = userService.updateUser(userId, userDTO);
        return Result.success("更新成功", updated);
    }
}
