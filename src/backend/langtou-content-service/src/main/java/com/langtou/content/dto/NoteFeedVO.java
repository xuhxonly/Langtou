package com.langtou.content.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Feed流笔记卡片
 */
@Data
public class NoteFeedVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    /**
     * 文本内容摘要（前100字）
     */
    private String summary;

    /**
     * 首图URL（用于Feed流展示）
     */
    private String coverImage;

    private List<String> tags;

    private Integer contentType;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    private LocalDateTime createTime;

    // ===== 作者简要信息 =====

    private String authorNickname;

    private String authorAvatar;

    // ===== LBS附近笔记距离信息 =====

    /**
     * 与查询位置的距离（米），仅附近笔记查询时返回
     */
    private Integer distance;

    // ===== 答题闯关徽章 =====

    private Boolean quizEnabled;

    private Long quizSetId;

    private String quizStatus;
}

