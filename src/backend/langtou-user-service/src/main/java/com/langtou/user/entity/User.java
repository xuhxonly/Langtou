package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String avatarUrl;

    private String email;

    private String phone;

    private Integer gender;

    private String bio;

    private String birthday;

    private String location;

    private Integer followerCount;

    private Integer followingCount;

    private Integer noteCount;

    private Integer likedCount;

    private Integer status;

    /**
     * 是否已验证年龄: 0-未验证 1-已验证
     */
    private Integer ageVerified;

    /**
     * 验证后的年龄
     */
    private Integer verifiedAge;

    /**
     * 青少年模式是否开启: 0-关闭 1-开启
     */
    private Integer teenModeEnabled;

    /**
     * 青少年模式PIN码(加密存储)
     */
    private String teenModePin;

    /**
     * 当日已使用时长(秒)
     */
    private Integer dailyUsageSeconds;

    /**
     * 最后使用日期(用于每日重置)
     */
    private LocalDate lastUsageDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
