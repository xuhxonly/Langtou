package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.langtou.quiz.enums.QuizAttemptStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_attempt")
public class QuizAttempt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizSetId;

    private Long userId;

    private Long gameSessionId;

    private Integer totalQuestions;

    private Integer correctCount;

    private Integer score;

    private Integer livesLeft;

    private Integer revivesUsed;

    private QuizAttemptStatus status;

    private Boolean passed;

    private Integer durationSeconds;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}