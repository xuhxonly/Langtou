package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.common.client.UserClient;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.Result;
import com.langtou.content.dto.NoteFeedVO;
import com.langtou.content.entity.Content;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.service.SearchService;
import com.langtou.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ContentMapper contentMapper;
    private final UserClient userClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SEARCH_HISTORY_PREFIX = "search:history:";
    private static final String HOT_SEARCH_KEY = "search:hot:keywords";
    private static final int MAX_SEARCH_HISTORY = 20;
    private static final long SEARCH_HISTORY_TTL_DAYS = 30;

    @Override
    public PageResult<NoteFeedVO> searchNotes(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        Page<Content> pageParam = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.eq("status", CommonConstants.STATUS_ENABLE)
                .and(w -> w.like("title", keyword)
                        .or()
                        .like("content", keyword))
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);

        List<NoteFeedVO> records = result.getRecords().stream()
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public List<UserDTO> searchUsers(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        try {
            Result<List<UserDTO>> result = userClient.searchUsers(keyword, limit);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("搜索用户失败: keyword={}, error={}", keyword, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getHotSearchKeywords(int limit) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(HOT_SEARCH_KEY);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception e) {
            log.warn("读取热门搜索关键词失败: error={}", e.getMessage());
        }

        // MVP阶段返回空列表，后续可基于搜索量统计
        return Collections.emptyList();
    }

    @Override
    public void recordSearchHistory(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }

        String key = SEARCH_HISTORY_PREFIX + userId;
        try {
            // 移除已存在的相同关键词
            stringRedisTemplate.opsForList().remove(key, 0, keyword);
            // 将新关键词添加到列表头部
            stringRedisTemplate.opsForList().leftPush(key, keyword);
            // 只保留最近20条
            stringRedisTemplate.opsForList().trim(key, 0, MAX_SEARCH_HISTORY - 1);
            // 设置过期时间
            stringRedisTemplate.expire(key, SEARCH_HISTORY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录搜索历史失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
        }
    }

    @Override
    public List<String> getSearchHistory(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        String key = SEARCH_HISTORY_PREFIX + userId;
        try {
            return stringRedisTemplate.opsForList().range(key, 0, MAX_SEARCH_HISTORY - 1);
        } catch (Exception e) {
            log.warn("获取搜索历史失败: userId={}, error={}", userId, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public void clearSearchHistory(Long userId) {
        if (userId == null) {
            return;
        }
        String key = SEARCH_HISTORY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        log.info("搜索历史已清除: userId={}", userId);
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

        // 通过Feign获取真实用户信息
        fillFeedAuthorInfo(vo, content.getUserId());

        return vo;
    }

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
}
