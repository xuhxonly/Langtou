package com.langtou.quiz.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.langtou.quiz.enums.QuestionType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quiz_question")
public class QuizQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long quizSetId;

    private Integer sequenceNo;

    private String stem;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctAnswer;

    private QuestionType questionType;

    private String explanation;

    private Integer score;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}