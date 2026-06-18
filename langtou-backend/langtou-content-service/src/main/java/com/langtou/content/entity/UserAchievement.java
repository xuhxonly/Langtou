package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户成就实体
 */
@Data
@TableName("user_achievements")
public class UserAchievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 成就类型: FIRST_NOTE/FIRST_COMMENT/LIKE_MILESTONE/FOLLOWER_MILESTONE/CONTINUOUS_LOGIN/CONTENT_MILESTONE
     */
    private String achievementType;

    /**
     * 成就名称
     */
    private String achievementName;

    /**
     * 成就描述
     */
    private String description;

    /**
     * 成就图标URL
     */
    private String iconUrl;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockedAt;
}
