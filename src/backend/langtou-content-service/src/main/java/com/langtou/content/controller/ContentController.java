package com.langtou.content.controller;

import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.dto.AnalyticsEventDTO;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.dto.ContentSearchDTO;
import com.langtou.content.dto.NoteDetailVO;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.service.AnalyticsService;
import com.langtou.content.service.ContentService;
import com.langtou.content.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "内容服务", description = "笔记发布、编辑、搜索、推荐、文件上传等内容相关接口")
public class ContentController {

    private final ContentService contentService;
    private final SearchService searchService;
    private final AnalyticsService analyticsService;

    @PostMapping("/notes")
    @Operation(summary = "发布笔记", description = "发布新的笔记内容，支持图片、视频等富媒体")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<ContentDTO> publish(@Valid @RequestBody ContentDTO contentDTO,
                                      @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        ContentDTO result = contentService.publish(contentDTO, userId);
        return Result.success("发布成功", result);
    }

    @GetMapping("/notes")
    @Operation(summary = "获取笔记Feed流", description = "获取最新笔记列表，支持分页")
    public Result<PageResult<NoteFeedVO>> getNotes(
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getFeedPage(page, size);
        return Result.success(result);
    }

    @GetMapping("/notes/{noteId}")
    @Operation(summary = "获取笔记详情", description = "根据笔记ID获取笔记详细内容")
    public Result<NoteDetailVO> getNoteById(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        NoteDetailVO result = contentService.getNoteDetail(noteId);
        return Result.success(result);
    }

    @DeleteMapping("/notes/{noteId}")
    @Operation(summary = "删除笔记", description = "删除指定笔记（仅作者可删除）")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> deleteNote(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId) {
        contentService.deleteContent(noteId, userId);
        return Result.success("删除成功");
    }

    @PutMapping("/notes/{noteId}")
    @Operation(summary = "编辑笔记", description = "编辑指定笔记内容")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<ContentDTO> updateNote(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @Valid @RequestBody ContentDTO contentDTO) {
        ContentDTO result = contentService.updateContent(noteId, userId, contentDTO);
        return Result.success("更新成功", result);
    }

    @GetMapping("/users/{userId}/notes")
    @Operation(summary = "获取用户笔记列表", description = "获取指定用户的笔记列表，支持分页")
    public Result<PageResult<NoteFeedVO>> getUserNotes(
            @Parameter(name = "userId", description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getUserNotes(userId, page, size);
        return Result.success(result);
    }

    @GetMapping("/notes/recommended")
    @Operation(summary = "个性化推荐Feed流", description = "基于推荐算法返回个性化笔记Feed")
    public Result<PageResult<NoteFeedVO>> getRecommendedFeed(
            @Parameter(name = "userId", description = "用户ID（可选）")
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getRecommendedFeed(userId, page, size);
        return Result.success(result);
    }

    @GetMapping("/search")
    @Operation(summary = "搜索笔记", description = "根据关键词搜索笔记")
    public Result<PageResult<NoteFeedVO>> searchNotes(
            @Parameter(name = "keyword", description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(name = "type", description = "搜索类型") @RequestParam(defaultValue = "note") String type,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        ContentSearchDTO searchDTO = new ContentSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        PageResult<NoteFeedVO> result = contentService.searchNotes(searchDTO);
        return Result.success(result);
    }
    @PostMapping("/upload")
    @Operation(summary = "图片上传", description = "上传图片文件，返回可访问URL")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Map<String, String>> uploadFile(
            @Parameter(name = "file", description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        String fileUrl = contentService.uploadFile(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success("上传成功", data);
    }

    @PostMapping("/upload/video")
    @Operation(summary = "视频上传", description = "上传视频文件（最大500MB）")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Map<String, String>> uploadVideo(
            @Parameter(name = "file", description = "视频文件", required = true)
            @RequestParam("file") MultipartFile file) {
        String fileUrl = contentService.uploadVideo(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success("视频上传成功", data);
    }

    @GetMapping("/notes/following")
    @Operation(summary = "关注Feed流", description = "获取已关注用户的笔记Feed流")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<PageResult<NoteFeedVO>> getFollowingFeed(
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getFollowingFeed(userId, page, size);
        return Result.success(result);
    }

    @PostMapping("/notes/{noteId}/like-count/inc")
    @Operation(summary = "增加笔记点赞数（内部）", description = "内部服务调用")
    public Result<Void> incrementLikeCount(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        contentService.incrementLikeCount(noteId);
        return Result.success();
    }

    @PostMapping("/notes/{noteId}/like-count/dec")
    @Operation(summary = "减少笔记点赞数（内部）", description = "内部服务调用")
    public Result<Void> decrementLikeCount(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        contentService.decrementLikeCount(noteId);
        return Result.success();
    }

    @PostMapping("/notes/{noteId}/comment-count/inc")
    @Operation(summary = "增加笔记评论数（内部）", description = "内部服务调用")
    public Result<Void> incrementCommentCount(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        contentService.incrementCommentCount(noteId);
        return Result.success();
    }

    @PostMapping("/notes/{noteId}/collect-count/inc")
    @Operation(summary = "增加笔记收藏数（内部）", description = "内部服务调用")
    public Result<Void> incrementCollectCount(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        contentService.incrementCollectCount(noteId);
        return Result.success();
    }

    @PostMapping("/notes/{noteId}/collect-count/dec")
    @Operation(summary = "减少笔记收藏数（内部）", description = "内部服务调用")
    public Result<Void> decrementCollectCount(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId) {
        contentService.decrementCollectCount(noteId);
        return Result.success();
    }

    @GetMapping("/notes/{noteId}/related")
    @Operation(summary = "相关推荐笔记", description = "根据笔记ID获取相关推荐笔记列表")
    public Result<PageResult<NoteFeedVO>> getRelatedNotes(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        PageResult<NoteFeedVO> result = contentService.getRelatedNotes(noteId, page, size);
        return Result.success(result);
    }

    @GetMapping("/notes/nearby")
    @Operation(summary = "附近笔记查询", description = "基于经纬度查询指定范围内的笔记")
    public Result<PageResult<NoteFeedVO>> getNearbyNotes(
            @Parameter(name = "latitude", description = "纬度", required = true) @RequestParam Double latitude,
            @Parameter(name = "longitude", description = "经度", required = true) @RequestParam Double longitude,
            @Parameter(name = "radius", description = "搜索半径（米）") @RequestParam(defaultValue = "5000") Double radius,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "size", description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = searchService.searchNearbyNotes(latitude, longitude, radius, page, size);
        return Result.success(result);
    }

    @PostMapping("/analytics/events")
    @Operation(summary = "批量上报埋点事件", description = "接收客户端批量用户行为事件")
    public Result<Void> reportAnalyticsEvents(@RequestBody AnalyticsEventDTO.BatchRequest batchRequest) {
        if (batchRequest.getEvents() == null || batchRequest.getEvents().isEmpty()) {
            return Result.error("事件列表不能为空");
        }
        analyticsService.processBatchEvents(batchRequest.getEvents());
        return Result.success("埋点上报成功");
    }

    @PutMapping("/notes/{noteId}/visibility")
    @Operation(summary = "更新笔记可见性", description = "修改笔记的可见性")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> updateNoteVisibility(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestBody Map<String, Integer> body) {
        Integer visibility = body.get("visibility");
        contentService.updateNoteVisibility(noteId, userId, visibility);
        return Result.success("可见性更新成功");
    }

    @PutMapping("/notes/{noteId}/pin")
    @Operation(summary = "置顶/取消置顶笔记", description = "置顶或取消置顶指定笔记")
    @SecurityRequirement(name = "bearer-jwt")
    public Result<Void> pinNote(
            @Parameter(name = "noteId", description = "笔记ID", required = true) @PathVariable Long noteId,
            @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
            @RequestBody Map<String, Boolean> body) {
        Boolean pin = body.get("pin");
        contentService.pinNote(noteId, userId, pin != null && pin);
        return Result.success(pin != null && pin ? "置顶成功" : "取消置顶成功");
    }
}