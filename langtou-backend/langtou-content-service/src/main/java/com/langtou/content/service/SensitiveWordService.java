package com.langtou.content.service;

import com.langtou.content.entity.SensitiveWord;
import com.langtou.common.result.PageResult;

import java.util.List;

/**
 * 敏感词服务（MySQL + Redis 双层持久化）
 */
public interface SensitiveWordService {

    /**
     * 初始化：从MySQL加载所有启用敏感词到Redis
     */
    void initSensitiveWordsToRedis();

    /**
     * 添加敏感词（同步更新MySQL和Redis）
     */
    SensitiveWord addSensitiveWord(String word, String category, String source);

    /**
     * 删除敏感词（同步更新MySQL和Redis）
     */
    void deleteSensitiveWord(Long id);

    /**
     * 更新敏感词状态（启用/禁用，同步更新MySQL和Redis）
     */
    void updateSensitiveWordStatus(Long id, String status);

    /**
     * 分页查询敏感词列表
     */
    PageResult<SensitiveWord> getSensitiveWordPage(int page, int size, String word, String category, String source, String status);

    /**
     * 检查文本是否包含敏感词（优先查Redis）
     */
    boolean containsSensitiveWord(String text);

    /**
     * 获取所有启用的敏感词（优先从Redis）
     */
    List<String> getAllEnabledWords();

    /**
     * 获取所有敏感词（从MySQL）
     */
    List<SensitiveWord> getAllSensitiveWords();
}
