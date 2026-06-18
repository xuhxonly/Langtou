package com.langtou.content.service;

import com.langtou.content.entity.Content;
import com.langtou.content.service.impl.AuditResult;

import java.util.List;
import java.util.Map;

/**
 * 内容审核服务
 */
public interface ContentAuditService {

    /**
     * 内容审核（敏感词、图片数量、内容长度、发布频率）
     * @return 审核通过返回true，不通过返回false
     */
    boolean checkContent(Content content, Long userId);

    /**
     * 敏感词检测
     */
    boolean containsSensitiveWords(String text);

    /**
     * 获取敏感词列表
     */
    List<String> getSensitiveWords();

    /**
     * 添加敏感词
     */
    void addSensitiveWord(String word);

    /**
     * 删除敏感词
     */
    void removeSensitiveWord(Long wordId);

    /**
     * AI 内容审核（文本+图片+视频）
     * 集成阿里云/腾讯云内容安全 API，返回详细审核结果
     *
     * @param content  内容实体
     * @param userId   用户ID
     * @return AI 审核结果（通过/拒绝/人工复核）
     */
    AuditResult aiAuditContent(Content content, Long userId);

    /**
     * AI 图片审核
     *
     * @param imageUrl 图片URL
     * @return AI 审核结果
     */
    AuditResult aiAuditImage(String imageUrl);

    /**
     * AI 视频审核
     *
     * @param videoUrl 视频URL
     * @return AI 审核结果
     */
    AuditResult aiAuditVideo(String videoUrl);

    /**
     * 批量 AI 图片审核
     *
     * @param imageUrls 图片URL列表
     * @return 各图片审核结果映射
     */
    Map<String, AuditResult> batchAiAuditImages(List<String> imageUrls);
}
