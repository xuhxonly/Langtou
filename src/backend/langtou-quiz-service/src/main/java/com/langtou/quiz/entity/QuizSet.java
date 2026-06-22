package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.langtou.quiz.enums.QuizSetStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "quiz_set", autoResultMap = true)
public class QuizSet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long creatorId;

    private String title;

    private String coverUrl;

    private Integer questionCount;

    private QuizSetStatus status;

    private String source;

    private String promptHash;

    private Integer correctRate;

    @TableField(exist = false)
    private Integer playCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
