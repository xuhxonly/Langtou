package com.langtou.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 青少年模式配置实体（映射到user表的青少年模式字段）
 */
@Data
public class TeenModeConfig {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否已验证年龄
     */
    private Integer ageVerified;

    /**
     * 验证后的年龄
     */
    private Integer verifiedAge;

    /**
     * 青少年模式是否开启
     */
    private Integer teenModeEnabled;

    /**
     * 每日使用时长限制（秒），默认40分钟=2400秒
     */
    private Integer dailyUsageLimit;

    /**
     * 当日已使用时长（秒）
     */
    private Integer dailyUsageSeconds;

    /**
     * 最后使用日期
     */
    private String lastUsageDate;

    /**
     * 夜间禁用开始时间（小时），默认22
     */
    private Integer nightRestrictionStart;

    /**
     * 夜间禁用结束时间（小时），默认6
     */
    private Integer nightRestrictionEnd;

    /**
     * 是否启用夜间禁用
     */
    private Integer nightRestrictionEnabled;

    /**
     * 内容分级过滤上限：ALL/7+/12+/18+
     */
    private String contentRatingLimit;
}
