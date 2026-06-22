package com.langtou.ad.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

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
