package com.langtou.interact.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("share_record")
public class ShareRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long noteId;

    /**
     * 分享类型: link链接 image图片 wechat微信
     */
    private String shareType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
