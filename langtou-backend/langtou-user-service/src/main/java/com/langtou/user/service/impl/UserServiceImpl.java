package com.langtou.user.service.impl;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.common.utils.JwtUtils;
import com.langtou.common.utils.RedisKeyUtil;
import com.langtou.user.dto.SmsLoginDTO;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserProfileVO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.UserMapper;
import com.langtou.user.service.FollowService;
import com.langtou.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;

    @Override
    public UserDTO register(UserRegisterDTO registerDTO) {
        // 校验两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次密码不一致");
        }

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
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setPhone(registerDTO.getPhone());
        user.setNickname(StringUtils.hasText(registerDTO.getNickname()) ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setAvatarUrl(CommonConstants.DEFAULT_AVATAR);
        user.setStatus(CommonConstants.STATUS_ENABLE);
        user.setGender(CommonConstants.GENDER_UNKNOWN);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setNoteCount(0);
        user.setLikedCount(0);

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

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        if (!CommonConstants.STATUS_ENABLE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        log.info("用户登录成功: username={}", user.getUsername());
        return JwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public String smsLogin(SmsLoginDTO smsLoginDTO) {
        String phone = smsLoginDTO.getPhone();
        String code = smsLoginDTO.getCode();

        // 从Redis获取验证码进行校验
        String redisKey = SMS_CODE_PREFIX + phone;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(cachedCode) || !cachedCode.equals(code)) {
            log.warn("短信验证码校验失败: phone={}", phone);
            throw new BusinessException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }

        // 验证成功后删除验证码
        stringRedisTemplate.delete(redisKey);

        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            // 手机号未注册，自动创建账号
            user = new User();
            user.setUsername("user_" + phone.substring(7));
            user.setNickname("用户" + phone.substring(7));
            user.setPhone(phone);
            user.setAvatarUrl(CommonConstants.DEFAULT_AVATAR);
            user.setStatus(CommonConstants.STATUS_ENABLE);
            user.setGender(CommonConstants.GENDER_UNKNOWN);
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            user.setNoteCount(0);
            user.setLikedCount(0);
            userMapper.insert(user);
            log.info("手机号自动注册成功: phone={}", phone);
        }

        if (!CommonConstants.STATUS_ENABLE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        log.info("手机号登录成功: phone={}, userId={}", phone, user.getId());
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
            user.setAvatarUrl(userDTO.getAvatar());
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

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setAvatar(user.getAvatarUrl());
        dto.setFollowerCount(user.getFollowerCount() != null ? user.getFollowerCount().longValue() : 0L);
        dto.setFollowingCount(user.getFollowingCount() != null ? user.getFollowingCount().longValue() : 0L);
        dto.setNoteCount(user.getNoteCount() != null ? user.getNoteCount().longValue() : 0L);
        return dto;
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.userProfileKey(userId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, UserProfileVO.class);
            }
        } catch (Exception e) {
            log.warn("读取用户资料缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setFollowerCount(user.getFollowerCount() != null ? user.getFollowerCount().longValue() : 0L);
        vo.setFollowingCount(user.getFollowingCount() != null ? user.getFollowingCount().longValue() : 0L);
        vo.setNoteCount(user.getNoteCount() != null ? user.getNoteCount().longValue() : 0L);

        // 写入Redis缓存（TTL 30分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(RedisKeyUtil.USER_PROFILE_TTL));
        } catch (Exception e) {
            log.warn("写入用户资料缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return vo;
    }

    @Override
    public UserProfileVO updateProfile(Long userId, UserDTO userDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (StringUtils.hasText(userDTO.getNickname())) {
            user.setNickname(userDTO.getNickname());
        }
        if (StringUtils.hasText(userDTO.getAvatar())) {
            user.setAvatarUrl(userDTO.getAvatar());
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
        log.info("用户资料更新成功: userId={}", userId);
        return getUserProfile(userId);
    }

    @Override
    public String uploadAvatar(Long userId, String fileUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        user.setAvatarUrl(fileUrl);
        userMapper.updateById(user);
        log.info("用户头像更新成功: userId={}, avatar={}", userId, fileUrl);
        return fileUrl;
    }

    @Override
    public String refreshToken(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (!CommonConstants.STATUS_ENABLE.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        log.info("Token刷新成功: userId={}", userId);
        return JwtUtils.generateToken(user.getId(), user.getUsername());
    }

    @Override
    public List<Long> getFollowingIds(Long userId) {
        return followService.getFollowingIds(userId);
    }

    @Override
    public String sendSmsCode(String phone) {
        // 生成6位随机验证码
        String code = String.format("%06d", new java.util.Random().nextInt(1000000));

        // 存入Redis，TTL 5分钟
        String redisKey = SMS_CODE_PREFIX + phone;
        stringRedisTemplate.opsForValue().set(redisKey, code, SMS_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("短信验证码已生成: phone={}, code={}", phone, code);

        // MVP阶段直接返回验证码，生产环境接入短信服务商后移除此返回
        return code;
    }

    @Override
    public List<UserDTO> searchUsers(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.and(w -> w.like("nickname", keyword).or().like("username", keyword))
                .eq("status", CommonConstants.STATUS_ENABLE)
                .last("LIMIT " + limit);

        List<User> users = userMapper.selectList(wrapper);
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
}
