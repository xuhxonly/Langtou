package com.langtou.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就DTO
 */
@Data
public class UserAchievementDTO {

    private Long id;

    private Long userId;

    private String achievementType;

    private String achievementName;

    private String description;

    private String iconUrl;

    private LocalDateTime obtainedAt;
}
