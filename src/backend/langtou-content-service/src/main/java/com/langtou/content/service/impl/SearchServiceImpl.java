package com.langtou.content.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
import com.langtou.content.mapper.NoteTagMapper;
import com.langtou.content.mapper.TagMapper;
import com.langtou.content.service.SearchService;
import com.langtou.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
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
    private final ElasticsearchClient elasticsearchClient;
    private final NoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;

    private static final String SEARCH_HISTORY_PREFIX = "search:history:";
    private static final String HOT_SEARCH_KEY = "search:hot:keywords";
    private static final int MAX_SEARCH_HISTORY = 20;
    private static final long SEARCH_HISTORY_TTL_DAYS = 30;
    private static final String NOTE_INDEX = "langtou_notes";
    private static final String USER_INDEX = "langtou_users";

    @Override
    public PageResult<NoteFeedVO> searchNotes(String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        // 优先使用ES搜索
        try {
            PageResult<NoteFeedVO> esResult = searchNotesByES(keyword, page, size);
            if (!CollectionUtils.isEmpty(esResult.getList())) {
                return esResult;
            }
        } catch (Exception e) {
            log.warn("ES搜索笔记失败，降级到MySQL LIKE查询: keyword={}, error={}", keyword, e.getMessage());
        }

        // ES失败或结果为空，降级到MySQL LIKE查询
        return searchNotesByMySQL(keyword, page, size);
    }

    private PageResult<NoteFeedVO> searchNotesByES(String keyword, int page, int size) throws IOException {
        int from = (page - 1) * size;

        SearchRequest request = SearchRequest.of(s -> s
                .index(NOTE_INDEX)
                .from(from)
                .size(size)
                .query(q -> q
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("title^3", "content^2", "tags^2")
                        )
                )
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
        List<Long> noteIds = response.hits().hits().stream()
                .map(Hit::id)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        if (noteIds.isEmpty()) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        List<Content> contents = contentMapper.selectBatchIds(noteIds);
        Map<Long, Content> contentMap = contents.stream()
                .collect(Collectors.toMap(Content::getId, c -> c));

        List<NoteFeedVO> records = noteIds.stream()
                .map(contentMap::get)
                .filter(Objects::nonNull)
                .filter(c -> CommonConstants.STATUS_ENABLE.equals(c.getStatus()))
                .map(this::convertToFeedVO)
                .collect(Collectors.toList());

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        return PageResult.of(total, (long) page, (long) size, records);
    }

    private PageResult<NoteFeedVO> searchNotesByMySQL(String keyword, int page, int size) {
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

        // 批量填充作者信息，避免N+1问题
        fillFeedAuthorInfoBatch(records);

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public List<UserDTO> searchUsers(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        // 优先使用ES搜索
        try {
            List<UserDTO> esResult = searchUsersByES(keyword, limit);
            if (!esResult.isEmpty()) {
                return esResult;
            }
        } catch (Exception e) {
            log.warn("ES搜索用户失败，降级到Feign调用: keyword={}, error={}", keyword, e.getMessage());
        }

        // ES失败或结果为空，降级到Feign调用user-service
        return searchUsersByFeign(keyword, limit);
    }

    private List<UserDTO> searchUsersByES(String keyword, int limit) throws IOException {
        SearchRequest request = SearchRequest.of(s -> s
                .index(USER_INDEX)
                .size(limit)
                .query(q -> q
                        .multiMatch(m -> m
                                .query(keyword)
                                .fields("username^2", "nickname^3", "bio")
                        )
                )
        );

        SearchResponse<Map> response = elasticsearchClient.search(request, Map.class);
        List<UserDTO> users = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                UserDTO dto = new UserDTO();
                dto.setId(Long.valueOf(source.get("id").toString()));
                dto.setUsername((String) source.get("username"));
                dto.setNickname((String) source.get("nickname"));
                dto.setAvatar((String) source.get("avatar"));
                dto.setBio((String) source.get("bio"));
                users.add(dto);
            }
        }
        return users;
    }

    private List<UserDTO> searchUsersByFeign(String keyword, int limit) {
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
        return getHotSearchRank(limit);
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
            // 记录搜索词频次（用于热搜榜统计）
            recordSearchKeyword(keyword);
        } catch (Exception e) {
            log.warn("记录搜索历史失败: userId={}, keyword={}, error={}", userId, keyword, e.getMessage());
        }
    }

    private static final String SEARCH_FREQ_KEY = "search:freq";
    private static final String SEARCH_SUGGEST_PREFIX = "search:suggest:";

    @Override
    public List<String> getSearchSuggestions(String keyword, int limit) {
        if (!StringUtils.hasText(keyword) || keyword.length() < 1) {
            return Collections.emptyList();
        }
        try {
            // 从热搜词频中获取前缀匹配的词
            Set<String> candidates = stringRedisTemplate.opsForZSet()
                    .reverseRange(SEARCH_FREQ_KEY, 0, 99);
            if (CollectionUtils.isEmpty(candidates)) {
                return Collections.emptyList();
            }
            String lowerKeyword = keyword.toLowerCase();
            return candidates.stream()
                    .filter(s -> s.toLowerCase().startsWith(lowerKeyword))
                    .sorted()
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取搜索建议失败: keyword={}, error={}", keyword, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void recordSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().incrementScore(SEARCH_FREQ_KEY, keyword, 1);
        } catch (Exception e) {
            log.warn("记录搜索词频次失败: keyword={}, error={}", keyword, e.getMessage());
        }
    }

    @Override
    public List<String> getHotSearchRank(int limit) {
        try {
            Set<String> topKeywords = stringRedisTemplate.opsForZSet()
                    .reverseRange(SEARCH_FREQ_KEY, 0, limit - 1);
            if (CollectionUtils.isEmpty(topKeywords)) {
                return Collections.emptyList();
            }
            return new ArrayList<>(topKeywords);
        } catch (Exception e) {
            log.warn("获取热搜榜失败: error={}", e.getMessage());
            return Collections.emptyList();
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

    @Override
    public PageResult<NoteFeedVO> searchNearbyNotes(Double lat, Double lng, Double radius, int page, int size) {
        if (lat == null || lng == null || radius == null || radius <= 0) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        // 参数校验：纬度范围 [-90, 90]，经度范围 [-180, 180]
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("经纬度参数不合法");
        }

        // 限制最大搜索半径为50公里
        double maxRadius = 50000.0;
        if (radius > maxRadius) {
            radius = maxRadius;
        }

        try {
            // 使用Haversine公式计算距离，通过MySQL查询附近笔记
            // 优先尝试使用MySQL空间索引（ST_Distance_Sphere）
            String sql;
            List<Map<String, Object>> rows;

            try {
                // MySQL 8.0+ 支持ST_Distance_Sphere，使用空间索引加速
                sql = "SELECT n.*, " +
                      "ST_Distance_Sphere(ST_PointFromText(CONCAT('POINT(', n.longitude, ' ', n.latitude, ')'), 4326), " +
                      "ST_PointFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326)) AS distance " +
                      "FROM note n " +
                      "WHERE n.status = 1 " +
                      "AND n.latitude IS NOT NULL AND n.longitude IS NOT NULL " +
                      "AND ST_Distance_Sphere(ST_PointFromText(CONCAT('POINT(', n.longitude, ' ', n.latitude, ')'), 4326), " +
                      "ST_PointFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326)) <= ? " +
                      "ORDER BY distance ASC " +
                      "LIMIT ? OFFSET ?";
                int offset = (page - 1) * size;
                rows = jdbcTemplate.queryForList(sql, lng, lat, lng, lat, radius, size, offset);
            } catch (Exception e) {
                log.warn("空间索引查询失败，降级到Haversine公式计算: error={}", e.getMessage());
                // 降级方案：使用Haversine公式 + 普通索引
                sql = "SELECT n.*, " +
                      "(6371000 * ACOS(COS(RADIANS(?)) * COS(RADIANS(n.latitude)) * " +
                      "COS(RADIANS(n.longitude) - RADIANS(?)) + " +
                      "SIN(RADIANS(?)) * SIN(RADIANS(n.latitude)))) AS distance " +
                      "FROM note n " +
                      "WHERE n.status = 1 " +
                      "AND n.latitude IS NOT NULL AND n.longitude IS NOT NULL " +
                      "HAVING distance <= ? " +
                      "ORDER BY distance ASC " +
                      "LIMIT ? OFFSET ?";
                int offset = (page - 1) * size;
                rows = jdbcTemplate.queryForList(sql, lat, lng, lat, radius, size, offset);
            }

            if (rows.isEmpty()) {
                return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
            }

            // 转换为NoteFeedVO列表
            List<NoteFeedVO> records = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Content content = mapRowToContent(row);
                NoteFeedVO vo = convertToFeedVO(content);
                // 附加距离信息（米）
                Object distance = row.get("distance");
                if (distance != null) {
                    vo.setDistance(Math.round(((Number) distance).doubleValue()));
                }
                records.add(vo);
            }

            // 批量填充作者信息，避免N+1问题
            fillFeedAuthorInfoBatch(records);

            // 查询总数（简化：使用限制后的数量作为近似）
            long total = records.size() + ((page - 1) * size);
            if (records.size() == size) {
                // 可能还有更多数据，标记总数为当前偏移+返回数量+1
                total = (long) page * size + 1;
            }

            return PageResult.of(total, (long) page, (long) size, records);

        } catch (Exception e) {
            log.error("附近笔记查询失败: lat={}, lng={}, radius={}, error={}", lat, lng, radius, e.getMessage());
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }
    }

    /**
     * 将查询结果Map映射为Content实体
     */
    private Content mapRowToContent(Map<String, Object> row) {
        Content content = new Content();
        content.setId(((Number) row.get("id")).longValue());
        content.setUserId(((Number) row.get("user_id")).longValue());
        content.setTitle((String) row.get("title"));
        content.setContent((String) row.get("content"));
        content.setVideoUrl((String) row.get("video_url"));
        content.setCoverUrl((String) row.get("cover_url"));
        content.setLocation((String) row.get("location"));

        Object lat = row.get("latitude");
        if (lat != null) {
            content.setLatitude(((Number) lat).floatValue());
        }
        Object lng = row.get("longitude");
        if (lng != null) {
            content.setLongitude(((Number) lng).floatValue());
        }

        content.setLikeCount(((Number) row.getOrDefault("like_count", 0)).intValue());
        content.setCommentCount(((Number) row.getOrDefault("comment_count", 0)).intValue());
        content.setCollectCount(((Number) row.getOrDefault("collect_count", 0)).intValue());
        content.setViewCount(((Number) row.getOrDefault("view_count", 0)).intValue());
        content.setStatus(((Number) row.getOrDefault("status", 1)).intValue());

        Object createdAt = row.get("created_at");
        if (createdAt instanceof java.sql.Timestamp) {
            content.setCreatedAt(((java.sql.Timestamp) createdAt).toLocalDateTime());
        }
        return content;
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

        // 不在此处调用 fillFeedAuthorInfo，由调用方统一批量填充
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

    /**
     * 批量填充FeedVO的作者信息，避免N+1问题
     */
    private void fillFeedAuthorInfoBatch(List<NoteFeedVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        // 收集所有去重userId
        List<Long> userIds = records.stream()
                .map(NoteFeedVO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        // 批量查询用户信息
        Map<Long, Map<String, Object>> userMap = new HashMap<>();
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
}
