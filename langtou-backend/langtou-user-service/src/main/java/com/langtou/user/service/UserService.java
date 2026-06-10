package com.langtou.user.service;

import com.langtou.user.dto.UserDTO;
import com.langtou.user.dto.UserLoginDTO;
import com.langtou.user.dto.UserRegisterDTO;

public interface UserService {

    UserDTO register(UserRegisterDTO registerDTO);

    String login(UserLoginDTO loginDTO);

    UserDTO getUserById(Long id);

    UserDTO getCurrentUser(Long userId);

    UserDTO updateUser(Long userId, UserDTO userDTO);
}
