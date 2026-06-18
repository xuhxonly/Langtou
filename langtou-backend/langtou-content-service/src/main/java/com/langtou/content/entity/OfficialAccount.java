package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 官方账号实体
 */
@Data
@TableName("official_accounts")
public class OfficialAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 账号类型: OFFICIAL/VERIFIED_CREATOR/BRAND
     */
    private String accountType;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 账号简介
     */
    private String description;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 认证标识: BLUE_V/GOLD_V
     */
    private String verifiedBadge;

    /**
     * 状态: ACTIVE/SUSPENDED
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
