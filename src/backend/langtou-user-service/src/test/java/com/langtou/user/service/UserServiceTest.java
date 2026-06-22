package com.langtou.user.service;

import com.langtou.common.exception.BusinessException;
import com.langtou.user.dto.SmsLoginDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserRegisterDTO;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.UserMapper;
import com.langtou.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 * 使用 Mockito 模拟依赖，不依赖 Spring 容器和数据库
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private FollowService followService;

    @Mock
    private SmsService smsService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setPasswordHash("encoded_password");
        testUser.setPhone("13800138000");
        testUser.setStatus(1);
        testUser.setAvatarUrl("https://cdn.langtou.com/default-avatar.png");
        testUser.setGender(0);
        testUser.setFollowerCount(0);
        testUser.setFollowingCount(0);
        testUser.setNoteCount(0);
        testUser.setLikedCount(0);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 登录测试 ====================

    @Test
    void testLogin_Success() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        String token = userService.login(loginDTO);
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(userMapper).selectByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encoded_password");
    }

    @Test
    void testLogin_WrongPassword() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("wrong_password");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(loginDTO));
    }

    @Test
    void testLogin_UserNotFound() {
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("notexist");
        loginDTO.setPassword("password123");

        when(userMapper.selectByUsername("notexist")).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.login(loginDTO));
    }

    @Test
    void testLogin_UserDisabled() {
        testUser.setStatus(0);
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.login(loginDTO));
    }

    // ==================== 注册测试 ====================

    @Test
    void testRegister_Success() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setPhone("13800138001");
        registerDTO.setNickname("新用户");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(userMapper.selectByPhone("13800138001")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_new_password");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        var result = userService.register(registerDTO);
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.register(registerDTO));
    }

    @Test
    void testRegister_PasswordMismatch() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("different_password");

        assertThrows(BusinessException.class, () -> userService.register(registerDTO));
    }

    @Test
    void testRegister_DuplicatePhone() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setPhone("13800138000");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.register(registerDTO));
    }

    // ==================== 短信验证码测试 ====================

    @Test
    void testSendSmsCode() {
        userService.sendSmsCode("13800138000");

        // 验证验证码已存入 Redis
        verify(valueOperations).set(startsWith("sms:code:"), anyString(), anyLong(), any());
        // 验证短信服务被调用
        verify(smsService).sendSmsCode(eq("13800138000"), anyString());
    }

    // ==================== 短信登录测试 ====================

    @Test
    void testSmsLogin_Success() {
        when(valueOperations.get("sms:code:13800138000")).thenReturn("123456");
        // 新用户（手机号未注册）
        when(userMapper.selectByPhone("13800138000")).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        String token = userService.smsLogin(dto);
        assertNotNull(token);
        // 验证码使用后应被删除
        verify(stringRedisTemplate).delete("sms:code:13800138000");
        // 新用户应自动注册
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testSmsLogin_ExistingUser() {
        when(valueOperations.get("sms:code:13800138000")).thenReturn("123456");
        when(userMapper.selectByPhone("13800138000")).thenReturn(testUser);

        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        String token = userService.smsLogin(dto);
        assertNotNull(token);
        // 已有用户不应再插入
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void testSmsLogin_WrongCode() {
        when(valueOperations.get("sms:code:13800138000")).thenReturn("654321");

        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        assertThrows(BusinessException.class, () -> userService.smsLogin(dto));
    }

    @Test
    void testSmsLogin_ExpiredCode() {
        when(valueOperations.get("sms:code:13800138000")).thenReturn(null);

        SmsLoginDTO dto = new SmsLoginDTO();
        dto.setPhone("13800138000");
        dto.setCode("123456");

        assertThrows(BusinessException.class, () -> userService.smsLogin(dto));
    }

    // ==================== 获取用户信息测试 ====================

    @Test
    void testGetUserById_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        var result = userService.getUserById(1L);
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("测试用户", result.getNickname());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.getUserById(999L));
    }

    // ==================== 修改密码测试 ====================

    @Test
    void testChangePassword_Success() {
        var changePasswordDTO = new com.langtou.user.dto.ChangePasswordDTO();
        changePasswordDTO.setOldPassword("password123");
        changePasswordDTO.setNewPassword("new_password");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");

        userService.changePassword(1L, changePasswordDTO);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        var changePasswordDTO = new com.langtou.user.dto.ChangePasswordDTO();
        changePasswordDTO.setOldPassword("wrong_password");
        changePasswordDTO.setNewPassword("new_password");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.changePassword(1L, changePasswordDTO));
    }

    @Test
    void testChangePassword_SamePassword() {
        var changePasswordDTO = new com.langtou.user.dto.ChangePasswordDTO();
        changePasswordDTO.setOldPassword("password123");
        changePasswordDTO.setNewPassword("password123");

        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.changePassword(1L, changePasswordDTO));
    }
}
