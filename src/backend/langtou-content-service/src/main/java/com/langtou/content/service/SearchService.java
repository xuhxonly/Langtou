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

    /**
     * LBS附近笔记查询
     * 基于经纬度查询指定半径范围内的笔记，按距离排序
     *
     * @param lat    纬度
     * @param lng    经度
     * @param radius 搜索半径（米）
     * @param page   页码
     * @param size   每页数量
     */
    PageResult<NoteFeedVO> searchNearbyNotes(Double lat, Double lng, Double radius, int page, int size);

    /**
     * 搜索建议（前缀匹配）
     */
    List<String> getSearchSuggestions(String keyword, int limit);

    /**
     * 记录搜索词频次（用于热搜榜统计）
     */
    void recordSearchKeyword(String keyword);

    /**
     * 获取热搜榜（基于搜索频次排序）
     */
    List<String> getHotSearchRank(int limit);
}
