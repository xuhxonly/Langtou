package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户等级实体
 */
@Data
@TableName(value = "user_levels", autoResultMap = true)
public class UserLevel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 等级名称
     */
    private String name;

    /**
     * 等级图标URL
     */
    private String iconUrl;

    /**
     * 最低积分
     */
    private Integer minPoints;

    /**
     * 最高积分
     */
    private Integer maxPoints;

    /**
     * 等级特权(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> privileges;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
