package com.langtou.content.service;

import com.langtou.common.exception.BusinessException;
import com.langtou.content.dto.ContentDTO;
import com.langtou.content.entity.Content;
import com.langtou.content.mapper.ContentMapper;
import com.langtou.content.service.impl.ContentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ContentService 单元测试
 * 使用 Mockito 模拟依赖，不依赖 Spring 容器和数据库
 */
@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private TagService tagService;

    @Mock
    private ContentAuditService contentAuditService;

    @InjectMocks
    private ContentServiceImpl contentService;

    private Content testContent;

    @BeforeEach
    void setUp() {
        testContent = new Content();
        testContent.setId(1L);
        testContent.setTitle("测试笔记");
        testContent.setContent("测试内容");
        testContent.setUserId(1L);
        testContent.setStatus(1);
        testContent.setViewCount(0);
        testContent.setLikeCount(0);
        testContent.setCommentCount(0);
        testContent.setCollectCount(0);
        testContent.setShareCount(0);
    }

    // ==================== 发布笔记测试 ====================

    @Test
    void testPublish_Success() {
        ContentDTO dto = new ContentDTO();
        dto.setTitle("新笔记");
        dto.setTextContent("新内容");
        dto.setContentType(1);

        when(contentAuditService.checkContent(any(Content.class), anyLong())).thenReturn(true);
        when(contentMapper.insert(any(Content.class))).thenReturn(1);

        ContentDTO result = contentService.publish(dto, 1L);
        assertNotNull(result);
        assertEquals("新笔记", result.getTitle());
        verify(contentMapper).insert(any(Content.class));
    }

    @Test
    void testPublish_AuditFailed() {
        ContentDTO dto = new ContentDTO();
        dto.setTitle("违规笔记");
        dto.setTextContent("违规内容");
        dto.setContentType(1);

        when(contentAuditService.checkContent(any(Content.class), anyLong())).thenReturn(false);
        when(contentMapper.insert(any(Content.class))).thenReturn(1);

        ContentDTO result = contentService.publish(dto, 1L);
        assertNotNull(result);
        // 审核不通过时 status 应为 0
        assertEquals(0, result.getStatus());
    }

    // ==================== 获取笔记详情测试 ====================

    @Test
    void testGetContentById_Success() {
        when(contentMapper.selectById(1L)).thenReturn(testContent);
        when(contentMapper.incrementViewCount(1L)).thenReturn(1);

        ContentDTO result = contentService.getContentById(1L);
        assertNotNull(result);
        assertEquals("测试笔记", result.getTitle());
        verify(contentMapper).incrementViewCount(1L);
    }

    @Test
    void testGetContentById_NotFound() {
        when(contentMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> contentService.getContentById(999L));
    }

    @Test
    void testGetContentById_DisabledContent() {
        testContent.setStatus(0);
        when(contentMapper.selectById(1L)).thenReturn(testContent);

        assertThrows(BusinessException.class, () -> contentService.getContentById(1L));
    }

    // ==================== 删除笔记测试 ====================

    @Test
    void testDeleteContent_Success() {
        when(contentMapper.selectById(1L)).thenReturn(testContent);
        when(contentMapper.deleteById(1L)).thenReturn(1);

        contentService.deleteContent(1L, 1L);
        verify(contentMapper).deleteById(1L);
    }

    @Test
    void testDeleteContent_NotFound() {
        when(contentMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> contentService.deleteContent(999L, 1L));
    }

    @Test
    void testDeleteContent_WrongOwner() {
        when(contentMapper.selectById(1L)).thenReturn(testContent);

        // 尝试用其他用户ID删除
        assertThrows(BusinessException.class, () -> contentService.deleteContent(1L, 2L));
        verify(contentMapper, never()).deleteById(anyLong());
    }

    // ==================== 获取用户内容列表测试 ====================

    @Test
    void testGetUserContents_Success() {
        when(contentMapper.selectByUserId(1L)).thenReturn(List.of(testContent));

        List<ContentDTO> result = contentService.getUserContents(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试笔记", result.get(0).getTitle());
    }

    @Test
    void testGetUserContents_Empty() {
        when(contentMapper.selectByUserId(999L)).thenReturn(List.of());

        List<ContentDTO> result = contentService.getUserContents(999L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 编辑笔记测试 ====================

    @Test
    void testUpdateContent_Success() {
        when(contentMapper.selectById(1L)).thenReturn(testContent);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        ContentDTO dto = new ContentDTO();
        dto.setTitle("更新后的标题");
        dto.setTextContent("更新后的内容");

        ContentDTO result = contentService.updateContent(1L, 1L, dto);
        assertNotNull(result);
        verify(contentMapper).updateById(any(Content.class));
    }

    @Test
    void testUpdateContent_NotFound() {
        when(contentMapper.selectById(999L)).thenReturn(null);

        ContentDTO dto = new ContentDTO();
        dto.setTitle("标题");

        assertThrows(BusinessException.class, () -> contentService.updateContent(999L, 1L, dto));
    }

    @Test
    void testUpdateContent_WrongOwner() {
        when(contentMapper.selectById(1L)).thenReturn(testContent);

        ContentDTO dto = new ContentDTO();
        dto.setTitle("标题");

        assertThrows(BusinessException.class, () -> contentService.updateContent(1L, 2L, dto));
        verify(contentMapper, never()).updateById(any(Content.class));
    }

    // ==================== 计数器操作测试 ====================

    @Test
    void testIncrementLikeCount() {
        contentService.incrementLikeCount(1L);
        verify(contentMapper).incrementLikeCount(1L);
    }

    @Test
    void testDecrementLikeCount() {
        contentService.decrementLikeCount(1L);
        verify(contentMapper).decrementLikeCount(1L);
    }

    @Test
    void testIncrementCommentCount() {
        contentService.incrementCommentCount(1L);
        verify(contentMapper).incrementCommentCount(1L);
    }

    @Test
    void testIncrementCollectCount() {
        contentService.incrementCollectCount(1L);
        verify(contentMapper).incrementCollectCount(1L);
    }

    @Test
    void testDecrementCollectCount() {
        contentService.decrementCollectCount(1L);
        verify(contentMapper).decrementCollectCount(1L);
    }
}
