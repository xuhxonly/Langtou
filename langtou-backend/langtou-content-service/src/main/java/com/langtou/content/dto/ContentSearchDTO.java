package com.langtou.content.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 内容搜索参数
 */
@Data
public class ContentSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 内容类型：1-笔记, 2-视频
     */
    private Integer contentType;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
