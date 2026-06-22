package com.langtou.content.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情视图对象（包含作者信息、互动数据）
 */
@Data
public class NoteDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private String textContent;

    private List<String> mediaUrls;
    private String videoUrl;

    private Integer contentType;

    private List<String> tags;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ===== 作者信息 =====

    private String authorName;

    private String authorNickname;

    private String authorAvatar;

    // ===== 当前用户互动状态 =====

    /**
     * 当前用户是否已点赞
     */
    private Boolean liked;

    /**
     * 当前用户是否已收藏
     */
    private Boolean collected;
}
