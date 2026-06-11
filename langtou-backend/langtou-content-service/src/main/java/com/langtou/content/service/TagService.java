package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Tag;

import java.util.List;

public interface TagService {

    /**
     * 获取热门标签（按使用次数倒序）
     */
    List<Tag> getHotTags(int limit);

    /**
     * 标签搜索（按名称模糊匹配）
     */
    List<Tag> searchTags(String keyword, int limit);

    /**
     * 获取标签下的笔记（分页）
     */
    PageResult<ContentDTO> getNotesByTagId(Long tagId, int page, int size);

    /**
     * 根据笔记ID获取标签列表
     */
    List<Tag> getTagsByNoteId(Long noteId);

    /**
     * 为笔记添加标签（如果标签不存在则创建）
     */
    void addTagsToNote(Long noteId, List<String> tagNames);

    /**
     * 获取或创建标签
     */
    Tag getOrCreateTag(String tagName);
}
