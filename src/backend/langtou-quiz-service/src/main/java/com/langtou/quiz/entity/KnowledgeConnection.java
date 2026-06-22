package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.langtou.quiz.enums.ConnectionTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_connection")
public class KnowledgeConnection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceQuestionId;

    private Long targetQuestionId;

    private ConnectionTypeEnum connectionType;

    private String topic;

    private String description;

    private Integer strength;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
