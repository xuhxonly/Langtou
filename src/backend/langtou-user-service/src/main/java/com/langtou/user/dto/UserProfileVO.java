package com.langtou.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String bio;

    private Integer gender;

    /**
     * 粉丝数
     */
    private Long followerCount;

    /**
     * 关注数
     */
    private Long followingCount;

    /**
     * 笔记数
     */
    private Long noteCount;
}
