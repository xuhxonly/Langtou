package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_hint_log")
public class QuizHintLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long quizSetId;

    private Long questionId;

    private Integer hintLevel;

    private String hintContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
