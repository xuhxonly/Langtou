package com.langtou.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContentDTO {

    private Long id;
    private Long userId;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String textContent;
    private List<String> mediaUrls;

    @NotNull(message = "内容类型不能为空")
    private Integer contentType;

    private List<String> tags;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private LocalDateTime createTime;
}
