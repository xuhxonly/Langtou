package com.langtou.content.controller;

import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Tag;
import com.langtou.content.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 热门标签
     */
    @GetMapping("/tags/hot")
    public Result<List<Tag>> getHotTags(@RequestParam(defaultValue = "20") int limit) {
        List<Tag> tags = tagService.getHotTags(limit);
        return Result.success(tags);
    }

    /**
     * 标签搜索
     */
    @GetMapping("/tags/search")
    public Result<List<Tag>> searchTags(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "20") int limit) {
        List<Tag> tags = tagService.searchTags(keyword, limit);
        return Result.success(tags);
    }

    /**
     * 标签下的笔记（分页）
     */
    @GetMapping("/tags/{tagId}/notes")
    public Result<PageResult<ContentDTO>> getNotesByTag(@PathVariable Long tagId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        PageResult<ContentDTO> result = tagService.getNotesByTagId(tagId, page, size);
        return Result.success(result);
    }
}
