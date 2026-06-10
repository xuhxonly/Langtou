package com.langtou.user.service.impl;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.common.utils.JwtUtils;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.UserMapper;
import com.langtou.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private static final String SALT = "langtou_salt_2024";

    @Override
    public UserDTO register(UserRegisterDTO registerDTO) {
        User existUser = userMapper.selectByUsername(registerDTO.getUsername());
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(registerDTO.getPhone())) {
            User phoneUser = userMapper.selectByPhone(registerDTO.getPhone());
            if (phoneUser != null) {
                throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "该手机号已被注册");
            }
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(encryptPassword(registerDTO.getPassword()));
        user.setPhone(registerDTO.getPhone());
        user.setNickname(StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setAvatar(CommonConstants.DEFAULT_AVATAR);
        user.setStatus(CommonConstants.STATUS_ENABLE);
        user.setGender(CommonConstants.GENDER_UNKNOWN);

        userMapper.insert(user);
        log.info("用户注册成功: username={}", user.getUsername());

        return convertToDTO(user);
    }

    @Override
    public String login(UserLoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        String encryptedPassword = encryptPassword(loginDTO.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        if (!CommonConstants.STATUS_ENABLE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        log.info("用户登录成功: username={}", user.getUsername());
        return JwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return convertToDTO(user);
    }

    @Override
    public UserDTO getCurrentUser(Long userId) {
        return getUserById(userId);
    }

    @Override
    public UserDTO updateUser(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(userDTO.getNickname())) {
            user.setNickname(userDTO.getNickname());
        }
        if (StringUtils.hasText(userDTO.getAvatar())) {
            user.setAvatar(userDTO.getAvatar());
        }
        if (StringUtils.hasText(userDTO.getBio())) {
            user.setBio(userDTO.getBio());
        }
        if (userDTO.getGender() != null) {
            user.setGender(userDTO.getGender());
        }
        if (StringUtils.hasText(userDTO.getEmail())) {
            user.setEmail(userDTO.getEmail());
        }

        userMapper.updateById(user);
        return convertToDTO(user);
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((password + SALT).getBytes(StandardCharsets.UTF_8));
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
