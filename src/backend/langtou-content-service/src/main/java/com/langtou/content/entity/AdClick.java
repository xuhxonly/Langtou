package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广告点击记录实体
 */
@Data
@TableName("ad_click")
public class AdClick {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adId;

    private Long userId;

    private Long noteId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
