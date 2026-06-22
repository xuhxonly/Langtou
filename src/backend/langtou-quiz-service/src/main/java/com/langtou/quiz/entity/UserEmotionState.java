﻿package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.langtou.quiz.enums.EmotionEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "user_emotion_state")
public class UserEmotionState {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private EmotionEnum currentEmotion;

    private EmotionEnum previousEmotion;

    private Integer consecutiveCorrect;

    private Integer consecutiveWrong;

    private Integer sessionQuestions;

    private Integer sessionCorrect;

    private Long avgResponseMs;

    private Long lastResponseMs;

    private Integer sessionDurationSeconds;

    private String trigger;

    private LocalDateTime detectedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
