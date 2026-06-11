package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.result.PageResult;
import com.langtou.common.utils.RedisKeyUtil;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Content;
import com.langtou.content.entity.NoteTag;
import com.langtou.content.entity.Tag;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.mapper.NoteTagMapper;
import com.langtou.content.mapper.TagMapper;
import com.langtou.content.service.TagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final NoteTagMapper noteTagMapper;
    private final ContentMapper contentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Tag> getHotTags(int limit) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.hotTagsKey(limit);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return objectMapper.readValue(cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Tag.class));
            }
        } catch (Exception e) {
            log.warn("读取热门标签缓存失败: limit={}, error={}", limit, e.getMessage());
        }

        Page<Tag> page = new Page<>(1, limit);
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("note_count");
        Page<Tag> result = tagMapper.selectPage(page, wrapper);
        List<Tag> tags = result.getRecords();

        // 写入Redis缓存（TTL 1小时）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(tags),
                    Duration.ofSeconds(RedisKeyUtil.HOT_TAGS_TTL));
        } catch (Exception e) {
            log.warn("写入热门标签缓存失败: limit={}, error={}", limit, e.getMessage());
        }

        return tags;
    }

    @Override
    public List<Tag> searchTags(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }

        Page<Tag> page = new Page<>(1, limit);
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.like("name", keyword)
                .orderByDesc("note_count");
        Page<Tag> result = tagMapper.selectPage(page, wrapper);
        return result.getRecords();
    }

    @Override
    public PageResult<ContentDTO> getNotesByTagId(Long tagId, int page, int size) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        List<Long> noteIds = noteTagMapper.selectNoteIdsByTagId(tagId);
        if (CollectionUtils.isEmpty(noteIds)) {
            return PageResult.of(0L, (long) page, (long) size, Collections.emptyList());
        }

        Page<Content> contentPage = new Page<>(page, size);
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        wrapper.in("id", noteIds)
                .eq("status", CommonConstants.STATUS_ENABLE)
                .orderByDesc("created_at");
        Page<Content> result = contentMapper.selectPage(contentPage, wrapper);

        List<ContentDTO> records = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public List<Tag> getTagsByNoteId(Long noteId) {
        List<Long> tagIds = noteTagMapper.selectTagIdsByNoteId(noteId);
        if (CollectionUtils.isEmpty(tagIds)) {
            return Collections.emptyList();
        }
        return tagMapper.selectBatchIds(tagIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addTagsToNote(Long noteId, List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }

        for (String tagName : tagNames) {
            if (!StringUtils.hasText(tagName)) {
                continue;
            }
            Tag tag = getOrCreateTag(tagName.trim());

            QueryWrapper<NoteTag> wrapper = new QueryWrapper<>();
            wrapper.eq("note_id", noteId)
                    .eq("tag_id", tag.getId());
            Long count = noteTagMapper.selectCount(wrapper);
            if (count == 0) {
                NoteTag noteTag = new NoteTag();
                noteTag.setNoteId(noteId);
                noteTag.setTagId(tag.getId());
                noteTagMapper.insert(noteTag);
                tag.setNoteCount(tag.getNoteCount() + 1);
                tagMapper.updateById(tag);
            }
        }
        log.info("笔记标签添加成功: noteId={}, tags={}", noteId, tagNames);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tag getOrCreateTag(String tagName) {
        Tag existTag = tagMapper.selectByName(tagName);
        if (existTag != null) {
            return existTag;
        }

        Tag tag = new Tag();
        tag.setName(tagName);
        tag.setNoteCount(0);
        tagMapper.insert(tag);
        log.info("标签创建成功: name={}", tagName);
        return tag;
    }

    private ContentDTO convertToDTO(Content content) {
        ContentDTO dto = new ContentDTO();
        dto.setId(content.getId());
        dto.setUserId(content.getUserId());
        dto.setTitle(content.getTitle());
        dto.setTextContent(content.getContent());
        dto.setStatus(content.getStatus());
        dto.setViewCount(content.getViewCount());
        dto.setLikeCount(content.getLikeCount());
        dto.setCommentCount(content.getCommentCount());
        dto.setCollectCount(content.getCollectCount());
        dto.setCreateTime(content.getCreatedAt());

        if (!CollectionUtils.isEmpty(content.getImages())) {
            dto.setMediaUrls(content.getImages());
        }
        return dto;
    }
}
