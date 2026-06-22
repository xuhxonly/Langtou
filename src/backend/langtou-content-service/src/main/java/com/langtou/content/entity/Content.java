package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "note", autoResultMap = true)
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String videoUrl;

    private String coverUrl;

    private String location;

    /**
     * 纬度（LBS附近笔记功能）
     */
    private Float latitude;

    /**
     * 经度（LBS附近笔记功能）
     */
    private Float longitude;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private Integer shareCount;

    private Integer viewCount;

    private Integer status;

    private Integer quizEnabled;

    private Long quizSetId;

    private String quizStatus;

    /**
     * 可见性: 0-公开, 1-私密, 2-粉丝可见
     */
    private Integer visibility;

    /**
     * 是否置顶
     */
    private Integer isPinned;

    /**
     * 置顶排序权重
     */
    private Integer pinOrder;

    /**
     * 年龄分级: ALL-全年龄 7+-7岁以上 12+-12岁以上 18+-18岁以上
     */
    private String ageRating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

