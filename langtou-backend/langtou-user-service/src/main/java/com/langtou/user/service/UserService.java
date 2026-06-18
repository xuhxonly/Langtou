package com.langtou.user.service;

import com.langtou.user.dto.BindPhoneDTO;
import com.langtou.user.dto.ChangePasswordDTO;
import com.langtou.user.dto.SmsLoginDTO;
import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserProfileVO;
import com.langtou.user.dto.UserRegisterDTO;

import java.util.List;

public interface UserService {

    UserDTO register(UserRegisterDTO registerDTO);

    String login(UserLoginDTO loginDTO);

    /**
     * 手机号验证码登录（不存在则自动注册）
     */
    String smsLogin(SmsLoginDTO smsLoginDTO);

    UserDTO getUserById(Long id);

    UserDTO getCurrentUser(Long userId);

    UserDTO updateUser(Long userId, UserDTO userDTO);

    /**
     * 获取用户公开资料（粉丝数、关注数、笔记数）
     */
    UserProfileVO getUserProfile(Long userId);

    /**
     * 修改个人资料（昵称、头像、简介等）
     */
    UserProfileVO updateProfile(Long userId, UserDTO userDTO);

    /**
     * 上传头像（返回头像URL）
     */
    String uploadAvatar(Long userId, String fileUrl);

    /**
     * 刷新Token
     */
    String refreshToken(Long userId);

    /**
     * 获取用户关注的所有用户ID
     */
    List<Long> getFollowingIds(Long userId);

    /**
     * 发送短信验证码
     * 生产环境应接入真实短信服务商（如阿里云短信、腾讯云短信等），
     * 当前为开发测试阶段，验证码仅存储在Redis中，不会返回给客户端。
     */
    void sendSmsCode(String phone);

    /**
     * 搜索用户（按昵称或用户名模糊匹配）
     */
    List<UserDTO> searchUsers(String keyword, int limit);

    /**
     * 修改密码
     */
    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 绑定/修改手机号
     */
    void bindPhone(Long userId, BindPhoneDTO bindPhoneDTO);
}
