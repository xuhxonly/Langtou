package com.langtou.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户推送设置实体
 */
@Data
@TableName("push_settings")
public class PushSettings {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 私信推送开关 */
    private Boolean privateMessageEnabled;

    /** 互动通知推送开关 */
    private Boolean interactionEnabled;

    /** 系统通知推送开关 */
    private Boolean systemEnabled;

    /** 营销推送开关 */
    private Boolean marketingEnabled;

    /** 免打扰开始时间 */
    private String quietHoursStart;

    /** 免打扰结束时间 */
    private String quietHoursEnd;

    /** 每日推送上限 */
    private Integer dailyLimit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
