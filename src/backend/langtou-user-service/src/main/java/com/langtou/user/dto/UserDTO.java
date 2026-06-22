package com.langtou.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private String bio;
    private Integer status;
    private Long followerCount;
    private Long followingCount;
    private Long noteCount;
    private LocalDateTime createTime;
}
