package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.constant.CommonConstants;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Content;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentMapper contentMapper;

    @Override
    public ContentDTO publish(ContentDTO contentDTO, Long userId) {
        Content content = new Content();
        BeanUtils.copyProperties(contentDTO, content);
        content.setUserId(userId);
        content.setStatus(CommonConstants.STATUS_ENABLE);
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setCollectCount(0);

        if (!CollectionUtils.isEmpty(contentDTO.getMediaUrls())) {
            content.setMediaUrls(String.join(",", contentDTO.getMediaUrls()));
        }
        if (!CollectionUtils.isEmpty(contentDTO.getTags())) {
            content.setTags(String.join(",", contentDTO.getTags()));
        }

        contentMapper.insert(content);
        log.info("内容发布成功: id={}, userId={}", content.getId(), userId);
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
                .orderByDesc("create_time");
        Page<Content> result = contentMapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private ContentDTO convertToDTO(Content content) {
        ContentDTO dto = new ContentDTO();
        BeanUtils.copyProperties(content, dto);
        if (StringUtils.hasText(content.getMediaUrls())) {
            dto.setMediaUrls(Arrays.asList(content.getMediaUrls().split(",")));
        }
        if (StringUtils.hasText(content.getTags())) {
            dto.setTags(Arrays.asList(content.getTags().split(",")));
        }
        return dto;
    }
}
