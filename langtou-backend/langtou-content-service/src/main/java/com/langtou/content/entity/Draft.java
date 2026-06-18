package com.langtou.content.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "draft", autoResultMap = true)
public class Draft {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private String videoUrl;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private String location;

    private Float latitude;

    private Float longitude;

    /**
     * 状态: 0-草稿, 1-自动保存
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
