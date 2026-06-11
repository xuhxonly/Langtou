package com.langtou.content.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.dto.ContentSearchDTO;
import com.langtou.content.dto.NoteDetailVO;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/notes")
    public Result<ContentDTO> publish(@Valid @RequestBody ContentDTO contentDTO,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        ContentDTO result = contentService.publish(contentDTO, userId);
        return Result.success("发布成功", result);
    }

    @GetMapping("/notes")
    public Result<PageResult<NoteFeedVO>> getNotes(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getFeedPage(page, size);
        return Result.success(result);
    }

    @GetMapping("/notes/{noteId}")
    public Result<NoteDetailVO> getNoteById(@PathVariable Long noteId) {
        NoteDetailVO result = contentService.getNoteDetail(noteId);
        return Result.success(result);
    }

    @DeleteMapping("/notes/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        contentService.deleteContent(noteId, userId);
        return Result.success("删除成功");
    }

    /**
     * 编辑笔记
     */
    @PutMapping("/notes/{noteId}")
    public Result<ContentDTO> updateNote(@PathVariable Long noteId,
                                           @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                           @RequestBody ContentDTO contentDTO) {
        ContentDTO result = contentService.updateContent(noteId, userId, contentDTO);
        return Result.success("更新成功", result);
    }

    /**
     * 用户的笔记列表（分页）
     */
    @GetMapping("/users/{userId}/notes")
    public Result<PageResult<NoteFeedVO>> getUserNotes(@PathVariable Long userId,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getUserNotes(userId, page, size);
        return Result.success(result);
    }

    /**
     * 笔记搜索
     */
    @GetMapping("/search")
    public Result<PageResult<NoteFeedVO>> searchNotes(@RequestParam String keyword,
                                                      @RequestParam(defaultValue = "note") String type,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        ContentSearchDTO searchDTO = new ContentSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        PageResult<NoteFeedVO> result = contentService.searchNotes(searchDTO);
        return Result.success(result);
    }

    /**
     * 图片/视频上传接口（返回URL）
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = contentService.uploadFile(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success("上传成功", data);
    }

    /**
     * 关注用户的Feed流（分页）
     */
    @GetMapping("/notes/following")
    public Result<PageResult<NoteFeedVO>> getFollowingFeed(@RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getFollowingFeed(userId, page, size);
        return Result.success(result);
    }

    /**
     * 增加笔记点赞数（内部接口）
     */
    @PostMapping("/notes/{noteId}/like-count/inc")
    public Result<Void> incrementLikeCount(@PathVariable Long noteId) {
        contentService.incrementLikeCount(noteId);
        return Result.success();
    }

    /**
     * 减少笔记点赞数（内部接口）
     */
    @PostMapping("/notes/{noteId}/like-count/dec")
    public Result<Void> decrementLikeCount(@PathVariable Long noteId) {
        contentService.decrementLikeCount(noteId);
        return Result.success();
    }

    /**
     * 增加笔记评论数（内部接口）
     */
    @PostMapping("/notes/{noteId}/comment-count/inc")
    public Result<Void> incrementCommentCount(@PathVariable Long noteId) {
        contentService.incrementCommentCount(noteId);
        return Result.success();
    }

    /**
     * 增加笔记收藏数（内部接口）
     */
    @PostMapping("/notes/{noteId}/collect-count/inc")
    public Result<Void> incrementCollectCount(@PathVariable Long noteId) {
        contentService.incrementCollectCount(noteId);
        return Result.success();
    }

    /**
     * 减少笔记收藏数（内部接口）
     */
    @PostMapping("/notes/{noteId}/collect-count/dec")
    public Result<Void> decrementCollectCount(@PathVariable Long noteId) {
        contentService.decrementCollectCount(noteId);
        return Result.success();
    }
}
