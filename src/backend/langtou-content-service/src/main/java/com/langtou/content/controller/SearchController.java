package com.langtou.content.controller;

import io.swagger.v3.oas.annotations.tags.Tag;


import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.service.SearchService;
import com.langtou.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "搜索服务", description = "搜索接口")
    public class SearchController {

    private final SearchService searchService;

    /**
     * 搜索笔记
     */
    @GetMapping("/notes")
    public Result<PageResult<NoteFeedVO>> searchNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId) {
        // 记录搜索历史
        searchService.recordSearchHistory(userId, keyword);
        // 记录搜索词频次（用于热搜榜统计）
        searchService.recordSearchKeyword(keyword);
        PageResult<NoteFeedVO> result = searchService.searchNotes(keyword, page, size);
        return Result.success(result);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/users")
    public Result<List<UserDTO>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        List<UserDTO> users = searchService.searchUsers(keyword, limit);
        return Result.success(users);
    }

    /**
     * 获取热门搜索关键词
     */
    @GetMapping("/hot")
    public Result<List<String>> getHotSearchKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        List<String> keywords = searchService.getHotSearchKeywords(limit);
        return Result.success(keywords);
    }

    /**
     * 搜索建议（前缀匹配）
     */
    @GetMapping("/suggest")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        List<String> suggestions = searchService.getSearchSuggestions(q, limit);
        return Result.success(suggestions);
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public Result<List<String>> getSearchHistory(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        List<String> history = searchService.getSearchHistory(userId);
        return Result.success(history);
    }

    /**
     * 清除搜索历史
     */
    @DeleteMapping("/history")
    public Result<Void> clearSearchHistory(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        searchService.clearSearchHistory(userId);
        return Result.success("搜索历史已清除");
    }
}
