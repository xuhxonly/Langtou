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

    @NotBlank(message = "内容不能为空")
    private String textContent;
    private List<String> mediaUrls;
    private String videoUrl;

    @NotNull(message = "内容类型不能为空")
    private Integer contentType;

    private List<String> tags;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Float latitude;
    private Float longitude;
    private LocalDateTime createTime;
}
