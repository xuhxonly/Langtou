package com.langtou.content.service;

import com.langtou.content.entity.Content;

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
}
