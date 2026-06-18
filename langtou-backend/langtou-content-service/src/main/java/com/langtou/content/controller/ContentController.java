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
    private final SearchService searchService;
    private final AnalyticsService analyticsService;

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
                                           @Valid @RequestBody ContentDTO contentDTO) {
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
     * 个性化推荐 Feed 流
     * 优先调用推荐服务，若推荐服务不可用或无结果则 fallback 到时间倒序 Feed
     *
     * @param userId 用户ID（从请求头获取，未登录用户传 null 或走冷启动）
     * @param page   页码
     * @param size   每页数量
     */
    @GetMapping("/notes/recommended")
    public Result<PageResult<NoteFeedVO>> getRecommendedFeed(
            @RequestHeader(value = CommonConstants.REQUEST_USER_ID, required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = contentService.getRecommendedFeed(userId, page, size);
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
     * 视频上传接口（限制视频格式和大小，最大500MB）
     */
    @PostMapping("/upload/video")
    public Result<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        String fileUrl = contentService.uploadVideo(file);
        Map<String, String> data = new HashMap<>();
        data.put("url", fileUrl);
        return Result.success("视频上传成功", data);
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

    /**
     * 相关推荐笔记
     */
    @GetMapping("/notes/{noteId}/related")
    public Result<PageResult<NoteFeedVO>> getRelatedNotes(@PathVariable Long noteId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        PageResult<NoteFeedVO> result = contentService.getRelatedNotes(noteId, page, size);
        return Result.success(result);
    }

    /**
     * LBS附近笔记查询
     * 基于经纬度查询指定半径范围内的笔记，按距离排序
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @param radius    搜索半径（米），默认5000米
     * @param page      页码
     * @param size      每页数量
     */
    @GetMapping("/notes/nearby")
    public Result<PageResult<NoteFeedVO>> getNearbyNotes(@RequestParam Double latitude,
                                                        @RequestParam Double longitude,
                                                        @RequestParam(defaultValue = "5000") Double radius,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        PageResult<NoteFeedVO> result = searchService.searchNearbyNotes(latitude, longitude, radius, page, size);
        return Result.success(result);
    }

    /**
     * 批量上报埋点事件
     * 接收客户端批量发送的用户行为事件，写入Kafka + MySQL
     *
     * @param batchRequest 包含events列表的请求体
     */
    @PostMapping("/analytics/events")
    public Result<Void> reportAnalyticsEvents(@RequestBody AnalyticsEventDTO.BatchRequest batchRequest) {
        if (batchRequest.getEvents() == null || batchRequest.getEvents().isEmpty()) {
            return Result.error("事件列表不能为空");
        }
        analyticsService.processBatchEvents(batchRequest.getEvents());
        return Result.success("埋点上报成功");
    }

    /**
     * 更新笔记可见性
     */
    @PutMapping("/notes/{noteId}/visibility")
    public Result<Void> updateNoteVisibility(@PathVariable Long noteId,
                                              @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                              @RequestBody Map<String, Integer> body) {
        Integer visibility = body.get("visibility");
        contentService.updateNoteVisibility(noteId, userId, visibility);
        return Result.success("可见性更新成功");
    }

    /**
     * 置顶/取消置顶笔记
     */
    @PutMapping("/notes/{noteId}/pin")
    public Result<Void> pinNote(@PathVariable Long noteId,
                                 @RequestHeader(CommonConstants.REQUEST_USER_ID) Long userId,
                                 @RequestBody Map<String, Boolean> body) {
        Boolean pin = body.get("pin");
        contentService.pinNote(noteId, userId, pin != null && pin);
        return Result.success(pin != null && pin ? "置顶成功" : "取消置顶成功");
    }
}
