package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.client.UserClient;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.common.result.ResultCode;
import com.langtou.common.utils.RedisKeyUtil;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.dto.ContentSearchDTO;
import com.langtou.content.dto.NoteDetailVO;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.entity.Content;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.service.ContentAuditService;
import com.langtou.content.service.ContentService;
import com.langtou.content.service.MinioService;
import com.langtou.content.service.RecommendationService;
import com.langtou.content.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentMapper contentMapper;
    private final TagService tagService;
    private final UserClient userClient;
    private final ContentAuditService contentAuditService;
    private final StringRedisTemplate stringRedisTemplate;
    private final MinioService minioService;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${langtou.upload.path:/data/uploads/langtou/}")
    private String uploadPath;

    @Value("${langtou.upload.url-prefix:/uploads/}")
    private String uploadUrlPrefix;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "mp4", "mov", "avi", "mkv");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContentDTO publish(ContentDTO contentDTO, Long userId) {
        Content content = new Content();
        BeanUtils.copyProperties(contentDTO, content);
        content.setUserId(userId);

        if (!CollectionUtils.isEmpty(contentDTO.getMediaUrls())) {
            content.setImages(contentDTO.getMediaUrls());
        }
        if (StringUtils.hasText(contentDTO.getVideoUrl())) {
            content.setVideoUrl(contentDTO.getVideoUrl());
        }

        // 内容审核
        boolean auditPassed = contentAuditService.checkContent(content, userId);
        if (!auditPassed) {
            // 审核不通过，设置status为0（待审核）
            content.setStatus(CommonConstants.STATUS_DISABLE);
            log.info("内容审核未通过，进入待审核状态: userId={}", userId);
        } else {
            content.setStatus(CommonConstants.STATUS_ENABLE);
        }

        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setCollectCount(0);
        content.setShareCount(0);

        // 写入经纬度（LBS附近笔记功能）
        if (contentDTO.getLatitude() != null) {
            content.setLatitude(contentDTO.getLatitude());
        }
        if (contentDTO.getLongitude() != null) {
            content.setLongitude(contentDTO.getLongitude());
        }

        contentMapper.insert(content);

        // 处理标签：解析tags字符串，创建或获取tag记录，建立note_tag关联
        if (!CollectionUtils.isEmpty(contentDTO.getTags())) {
            tagService.addTagsToNote(content.getId(), contentDTO.getTags());
        }

        log.info("内容发布成功: id={}, userId={}, status={}, tags={}", content.getId(), userId, content.getStatus(), contentDTO.getTags());
        return convertToDTO(content);
    }

    @Override
    public ContentDTO getContentById(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null || CommonConstants.STATUS_DISABLE.equals(content.getStatus())) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }
        contentMapper.incrementViewCount(id);
        return convertToDTO(content);
    }

    @Override
    public List<ContentDTO> getUserContents(Long userId) {
        List<Content> contents = contentMapper.selectByUserId(userId);
        return contents.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteContent(Long id, Long userId) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }
        if (!content.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该内容");
        }
        contentMapper.deleteById(id);
        log.info("内容删除成功: id={}", id);
    }

    @Override
    public List<ContentDTO> getFeed(int page, int size) {
        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ContentDTO convertToDTO(Content content) {
        ContentDTO dto = new ContentDTO();
        BeanUtils.copyProperties(content, dto);
        dto.setTextContent(content.getContent());
        if (!CollectionUtils.isEmpty(content.getImages())) {
            dto.setMediaUrls(content.getImages());
        }
        if (StringUtils.hasText(content.getVideoUrl())) {
            dto.setVideoUrl(content.getVideoUrl());
        }
        return dto;
    }

    @Override
    public PageResult<NoteFeedVO> getFeedPage(int page, int size) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.feedPageKey(page, size);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, PageResult.class);
            }
        } catch (Exception e) {
            log.warn("读取Feed缓存失败: page={}, size={}, error={}", page, size, e.getMessage());
        }

        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        List<NoteFeedVO> records = result.getRecords().stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        PageResult<NoteFeedVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);

        // 写入Redis缓存（TTL 5分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(pageResult),
                    Duration.ofSeconds(RedisKeyUtil.FEED_PAGE_TTL));
        } catch (Exception e) {
            log.warn("写入Feed缓存失败: page={}, size={}, error={}", page, size, e.getMessage());
        }

        return pageResult;
    }

    @Override
    public PageResult<NoteFeedVO> getRecommendedFeed(Long userId, int page, int size) {
        // 1. 优先调用推荐服务获取个性化推荐内容
        try {
            List<NoteFeedVO> recommended = recommendationService.recommendFeed(userId, page, size);
            if (!CollectionUtils.isEmpty(recommended)) {
                log.info("个性化推荐命中: userId={}, page={}, size={}, count={}", userId, page, size, recommended.size());
                // 推荐结果分页包装（推荐服务已做召回+排序，此处直接包装）
                return PageResult.of((long) recommended.size(), (long) page, (long) size, recommended);
            }
        } catch (Exception e) {
            log.warn("个性化推荐服务调用失败，fallback到时间倒序: userId={}, error={}", userId, e.getMessage());
        }

        // 2. Fallback：推荐服务不可用或无结果时，回退到时间倒序Feed
        log.info("个性化推荐fallback到时间倒序Feed: userId={}, page={}, size={}", userId, page, size);
        return getFeedPage(page, size);
    }

    @Override
    public NoteDetailVO getNoteDetail(Long noteId) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.noteDetailKey(noteId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, NoteDetailVO.class);
            }
        } catch (Exception e) {
            log.warn("读取笔记详情缓存失败: noteId={}, error={}", noteId, e.getMessage());
        }

        Content content = contentMapper.selectById(noteId);
        if (content == null || CommonConstants.STATUS_DISABLE.equals(content.getStatus())) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }

        contentMapper.incrementViewCount(noteId);

        NoteDetailVO vo = new NoteDetailVO();
        vo.setId(content.getId());
        vo.setUserId(content.getUserId());
        vo.setTitle(content.getTitle());
        vo.setTextContent(content.getContent());
        vo.setStatus(content.getStatus());
        vo.setViewCount(content.getViewCount() + 1);
        vo.setLikeCount(content.getLikeCount());
        vo.setCommentCount(content.getCommentCount());
        vo.setCollectCount(content.getCollectCount());
        vo.setCreateTime(content.getCreatedAt());
        vo.setUpdateTime(content.getUpdatedAt());

        if (!CollectionUtils.isEmpty(content.getImages())) {
            vo.setMediaUrls(content.getImages());
        }
        if (StringUtils.hasText(content.getVideoUrl())) {
            vo.setVideoUrl(content.getVideoUrl());
        }

        // 通过Feign获取真实用户信息
        fillAuthorInfo(vo, content.getUserId());

        vo.setLiked(false);
        vo.setCollected(false);

        // 写入Redis缓存（TTL 30分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo),
                    Duration.ofSeconds(RedisKeyUtil.NOTE_DETAIL_TTL));
        } catch (Exception e) {
            log.warn("写入笔记详情缓存失败: noteId={}, error={}", noteId, e.getMessage());
        }

        return vo;
    }

    /**
     * 通过Feign客户端填充作者信息
     */
    private void fillAuthorInfo(NoteDetailVO vo, Long userId) {
        try {
            Result<Map<String, Object>> result = userClient.getUserById(userId);
            if (result != null && result.getData() != null) {
                Map<String, Object> userMap = result.getData();
                vo.setAuthorName(userMap.get("username") != null ? userMap.get("username").toString() : "用户" + userId);
                vo.setAuthorNickname(userMap.get("nickname") != null ? userMap.get("nickname").toString() : "用户" + userId);
                vo.setAuthorAvatar(userMap.get("avatar") != null ? userMap.get("avatar").toString() : CommonConstants.DEFAULT_AVATAR);
            } else {
                vo.setAuthorName("用户" + userId);
                vo.setAuthorNickname("用户" + userId);
                vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId={}, error={}", userId, e.getMessage());
            vo.setAuthorName("用户" + userId);
            vo.setAuthorNickname("用户" + userId);
            vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
        }
    }

    /**
     * 通过Feign客户端填充FeedVO的作者信息（单个，已废弃，请使用批量方法）
     */
    private void fillFeedAuthorInfo(NoteFeedVO vo, Long userId) {
        try {
            Result<Map<String, Object>> result = userClient.getUserById(userId);
            if (result != null && result.getData() != null) {
                Map<String, Object> userMap = result.getData();
                vo.setAuthorNickname(userMap.get("nickname") != null ? userMap.get("nickname").toString() : "用户" + userId);
                vo.setAuthorAvatar(userMap.get("avatar") != null ? userMap.get("avatar").toString() : CommonConstants.DEFAULT_AVATAR);
            } else {
                vo.setAuthorNickname("用户" + userId);
                vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId={}, error={}", userId, e.getMessage());
            vo.setAuthorNickname("用户" + userId);
            vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
        }
    }

    /**
     * 批量填充FeedVO的作者信息，避免N+1问题
     */
    private void fillFeedAuthorInfoBatch(List<NoteFeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 收集所有userId
        List<Long> userIds = records.stream()
                .map(NoteFeedVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        // 批量查询用户信息
        Map<Long, Map<String, Object>> userMap = new java.util.HashMap<>();
        try {
            Result<List<Map<String, Object>>> result = userClient.batchGetUsers(userIds);
            if (result != null && result.getData() != null) {
                for (Map<String, Object> user : result.getData()) {
                    Object id = user.get("id");
                    if (id != null) {
                        userMap.put(Long.valueOf(id.toString()), user);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("批量获取用户信息失败, userIds={}, error={}", userIds, e.getMessage());
        }
        // 填充作者信息
        for (NoteFeedVO vo : records) {
            Long userId = vo.getUserId();
            Map<String, Object> user = userMap.get(userId);
            if (user != null) {
                vo.setAuthorNickname(user.get("nickname") != null ? user.get("nickname").toString() : "用户" + userId);
                vo.setAuthorAvatar(user.get("avatar") != null ? user.get("avatar").toString() : CommonConstants.DEFAULT_AVATAR);
            } else {
                vo.setAuthorNickname("用户" + userId);
                vo.setAuthorAvatar(CommonConstants.DEFAULT_AVATAR);
            }
        }
    }

    @Override
    public ContentDTO updateContent(Long noteId, Long userId, ContentDTO contentDTO) {
        Content content = contentMapper.selectById(noteId);
        if (content == null) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }
        if (!content.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权编辑该内容");
        }

        if (StringUtils.hasText(contentDTO.getTitle())) {
            content.setTitle(contentDTO.getTitle());
        }
        if (contentDTO.getTextContent() != null) {
            content.setContent(contentDTO.getTextContent());
        }
        if (!CollectionUtils.isEmpty(contentDTO.getMediaUrls())) {
            content.setImages(contentDTO.getMediaUrls());
        }
        if (contentDTO.getVideoUrl() != null) {
            content.setVideoUrl(contentDTO.getVideoUrl());
        }

        contentMapper.updateById(content);
        log.info("内容更新成功: id={}, userId={}", noteId, userId);
        return convertToDTO(content);
    }

    @Override
    public PageResult<NoteFeedVO> getUserNotes(Long userId, int page, int size) {
        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        List<NoteFeedVO> records = result.getRecords().stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public PageResult<NoteFeedVO> searchNotes(ContentSearchDTO searchDTO) {
        if (searchDTO == null || !StringUtils.hasText(searchDTO.getKeyword())) {
            return PageResult.of(0L, 1L, 20L, Collections.emptyList());
        }

        int page = searchDTO.getPage() != null ? searchDTO.getPage() : 1;
        int size = searchDTO.getSize() != null ? searchDTO.getSize() : 20;

        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE);

        wrapper.and(w -> w.like("title", searchDTO.getKeyword())
                .or()
                .like("content", searchDTO.getKeyword()));

        if (searchDTO.getUserId() != null) {
            wrapper.eq("user_id", searchDTO.getUserId());
        }

        wrapper.orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        List<NoteFeedVO> records = result.getRecords().stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件名不能为空");
        }

        // 获取文件扩展名
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        // 校验文件类型
        if (!ALLOWED_IMAGE_TYPES.contains(extension) && !ALLOWED_VIDEO_TYPES.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "不支持的文件类型: " + extension);
        }

        // 上传到MinIO并返回URL
        String fileUrl = minioService.uploadFile(file);
        log.info("文件上传成功: originalName={}, url={}", originalFilename, fileUrl);
        return fileUrl;
    }

    @Override
    public String uploadVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "上传视频不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件名不能为空");
        }

        // 获取文件扩展名
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        // 校验视频类型
        if (!ALLOWED_VIDEO_TYPES.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED, "不支持的视频格式: " + extension);
        }

        // 校验视频大小 (最大500MB)
        long maxVideoSize = 500 * 1024 * 1024;
        if (file.getSize() > maxVideoSize) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "视频大小不能超过500MB");
        }

        // 上传到MinIO并返回URL
        String fileUrl = minioService.uploadFile(file);
        log.info("视频上传成功: originalName={}, size={}, url={}", originalFilename, file.getSize(), fileUrl);
        return fileUrl;
    }

    private NoteFeedVO convertToFeedVO(Content content) {
        NoteFeedVO vo = new NoteFeedVO();
        vo.setId(content.getId());
        vo.setUserId(content.getUserId());
        vo.setTitle(content.getTitle());
        vo.setViewCount(content.getViewCount());
        vo.setLikeCount(content.getLikeCount());
        vo.setCommentCount(content.getCommentCount());
        vo.setCollectCount(content.getCollectCount());
        vo.setCreateTime(content.getCreatedAt());

        if (StringUtils.hasText(content.getContent())) {
            String text = content.getContent();
            vo.setSummary(text.length() > 100 ? text.substring(0, 100) + "..." : text);
        }

        if (!CollectionUtils.isEmpty(content.getImages())) {
            vo.setCoverImage(content.getImages().get(0));
        }
        if (StringUtils.hasText(content.getVideoUrl())) {
            vo.setCoverImage(content.getVideoUrl());
        }

        // 不在此处调用 fillFeedAuthorInfo，由调用方统一批量填充

        return vo;
    }

    @Override
    public void incrementLikeCount(Long noteId) {
        contentMapper.incrementLikeCount(noteId);
    }

    @Override
    public void decrementLikeCount(Long noteId) {
        contentMapper.decrementLikeCount(noteId);
    }

    @Override
    public void incrementCommentCount(Long noteId) {
        contentMapper.incrementCommentCount(noteId);
    }

    @Override
    public void incrementCollectCount(Long noteId) {
        contentMapper.incrementCollectCount(noteId);
    }

    @Override
    public void decrementCollectCount(Long noteId) {
        contentMapper.decrementCollectCount(noteId);
    }

    @Override
    public PageResult<NoteFeedVO> getRelatedNotes(Long noteId, int page, int size) {
        // 1. 获取当前笔记的标签ID列表
        List<Long> tagIds = tagService.getTagIdsByNoteId(noteId);
        if (tagIds.isEmpty()) {
            // 无标签时，返回同作者的其他笔记
            Content currentNote = contentMapper.selectById(noteId);
            if (currentNote == null) {
                throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
            }
            Page<Content> pageParam = new Page<>(page, size);
            QueryWrapper<Content> wrapper = new QueryWrapper<>();
            wrapper.eq("user_id", currentNote.getUserId())
                    .ne("id", noteId)
                    .eq("status", CommonConstants.STATUS_ENABLE)
                    .orderByDesc("created_at");
            Page<Content> result = contentMapper.selectPage(pageParam, wrapper);
            List<NoteFeedVO> records = result.getRecords().stream()
                    .map(this::convertToFeedVO)
                    .collect(Collectors.toList());
            // 批量填充作者信息，避免N+1问题
            fillFeedAuthorInfoBatch(records);
            return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
        }

        // 2. 获取拥有这些标签的其他笔记ID（去重，排除当前笔记）
        Set<Long> relatedNoteIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            List<Long> noteIds = tagService.getNoteIdsByTagId(tagId);
            if (noteIds != null) {
                relatedNoteIds.addAll(noteIds);
            }
        }
        relatedNoteIds.remove(noteId);

        if (relatedNoteIds.isEmpty()) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        // 3. 分页查询这些笔记
        List<Long> noteIdList = new ArrayList<>(relatedNoteIds);
        int from = (page - 1) * size;
        int to = Math.min(from + size, noteIdList.size());
        if (from >= noteIdList.size()) {
            return PageResult.of((long) noteIdList.size(), (long) page, (long) size, Collections.emptyList());
        }
        List<Long> pageNoteIds = noteIdList.subList(from, to);

        List<Content> contents = contentMapper.selectBatchIds(pageNoteIds);
        List<NoteFeedVO> records = contents.stream()
                .filter(c -> CommonConstants.STATUS_ENABLE.equals(c.getStatus()))
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        return PageResult.of((long) noteIdList.size(), (long) page, (long) size, records);
    }

    @Override
    public void updateNoteVisibility(Long noteId, Long userId, Integer visibility) {
        if (visibility == null || visibility < 0 || visibility > 2) {
            throw new BusinessException(ResultCode.VISIBILITY_INVALID);
        }
        Content content = contentMapper.selectById(noteId);
        if (content == null) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }
        if (!content.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权修改该笔记的可见性");
        }
        content.setVisibility(visibility);
        contentMapper.updateById(content);
        log.info("更新笔记可见性成功: noteId={}, visibility={}", noteId, visibility);
    }

    @Override
    public void pinNote(Long noteId, Long userId, boolean pin) {
        Content content = contentMapper.selectById(noteId);
        if (content == null) {
            throw new BusinessException(ResultCode.CONTENT_NOT_FOUND);
        }
        if (!content.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权置顶该笔记");
        }
        content.setIsPinned(pin ? 1 : 0);
        content.setPinOrder(pin ? 999 : 0);
        contentMapper.updateById(content);
        log.info("笔记置顶操作成功: noteId={}, pin={}", noteId, pin);
    }

    @Override
    public PageResult<NoteFeedVO> getFollowingFeed(Long userId, int page, int size) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.followingFeedKey(userId, page, size);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, PageResult.class);
            }
        } catch (Exception e) {
            log.warn("读取关注Feed缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        // 通过Feign获取当前用户关注的所有用户ID
        List<Long> followingIds;
        try {
            Result<List<Long>> result = userClient.getFollowingIds(userId);
            if (result != null && result.getData() != null) {
                followingIds = result.getData();
            } else {
                followingIds = Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("获取关注列表失败: userId={}, error={}", userId, e.getMessage());
            followingIds = Collections.emptyList();
        }

        if (followingIds.isEmpty()) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        // 查询这些用户发布的最新笔记
        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.in("user_id", followingIds)
                .eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        List<NoteFeedVO> records = result.getRecords().stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        PageResult<NoteFeedVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);

        // 写入Redis缓存（TTL 5分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(pageResult),
                    Duration.ofSeconds(RedisKeyUtil.FOLLOWING_FEED_TTL));
        } catch (Exception e) {
            log.warn("写入关注Feed缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return pageResult;
    }
}
