package com.langtou.interact.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 举报人ID
     */
    private Long reporterId;

    /**
     * 被举报的笔记ID
     */
    private Long noteId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 举报类型: spam-垃圾信息, illegal-违法违规, harassment-骚扰攻击, copyright-侵权, other-其他
     */
    private String reportType;

    /**
     * 处理状态: 0-待处理, 1-已处理
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
