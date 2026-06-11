package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.user.dto.UserDTO;

import java.util.List;

public interface SearchService {

    /**
     * 搜索笔记（MySQL LIKE查询，MVP阶段）
     */
    PageResult<NoteFeedVO> searchNotes(String keyword, int page, int size);

    /**
     * 搜索用户（MySQL LIKE查询，MVP阶段）
     */
    List<UserDTO> searchUsers(String keyword, int limit);

    /**
     * 获取热门搜索关键词
     */
    List<String> getHotSearchKeywords(int limit);

    /**
     * 记录搜索历史（存Redis，最近20条）
     */
    void recordSearchHistory(Long userId, String keyword);

    /**
     * 获取用户搜索历史
     */
    List<String> getSearchHistory(Long userId);

    /**
     * 清除用户搜索历史
     */
    void clearSearchHistory(Long userId);
}
