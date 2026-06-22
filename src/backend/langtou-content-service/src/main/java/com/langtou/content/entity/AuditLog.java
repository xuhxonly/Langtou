package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审核日志实体
 */
@Data
@TableName("audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 审核类型：text-文本审核, image-图片审核
     */
    private String auditType;

    /**
     * 审核对象ID（如笔记ID）
     */
    private Long targetId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 审核内容摘要
     */
    private String content;

    /**
     * 审核结果：pass-通过, reject-拒绝, review-人工复审
     */
    private String result;

    /**
     * 审核原因（拒绝时填写）
     */
    private String reason;

    /**
     * 审核服务商：local-本地, aliyun-阿里云, tencent-腾讯云等
     */
    private String provider;

    /**
     * 图片MD5（图片审核时用于缓存）
     */
    private String imageMd5;

    /**
     * 处理耗时（毫秒）
     */
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
