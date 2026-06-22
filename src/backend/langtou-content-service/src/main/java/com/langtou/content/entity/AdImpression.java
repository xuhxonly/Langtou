package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广告曝光记录实体
 */
@Data
@TableName("ad_impression")
public class AdImpression {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long adId;

    private Long userId;

    private Long noteId;

    private Integer position;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
