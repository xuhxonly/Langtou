package com.langtou.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推送日志实体
 */
@Data
@TableName("push_logs")
public class PushLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标用户ID */
    private Long userId;

    /** 目标设备Token */
    private String deviceToken;

    /** 推送类型: PRIVATE_MESSAGE / INTERACTION / SYSTEM / MARKETING */
    private String pushType;

    /** 推送标题 */
    private String title;

    /** 推送内容 */
    private String body;

    /** 推送附加数据(JSON) */
    private String data;

    /** 状态: PENDING / SENT / FAILED / DELIVERED */
    private String status;

    /** 发送时间 */
    private LocalDateTime sentAt;

    /** 到达时间 */
    private LocalDateTime deliveredAt;

    /** 失败错误信息 */
    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
