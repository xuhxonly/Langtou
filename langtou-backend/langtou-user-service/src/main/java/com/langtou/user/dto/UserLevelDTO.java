package com.langtou.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户等级/积分DTO
 */
@Data
public class UserLevelDTO {

    private Long userId;

    private Integer level;

    private Integer points;

    private Integer experience;

    private Integer totalPoints;

    private Integer nextLevelExperience;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
