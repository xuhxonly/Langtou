package com.langtou.ad.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
