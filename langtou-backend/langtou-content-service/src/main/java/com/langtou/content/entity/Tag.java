package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签图标
     */
    private String icon;

    /**
     * 关联笔记数
     */
    private Integer noteCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
