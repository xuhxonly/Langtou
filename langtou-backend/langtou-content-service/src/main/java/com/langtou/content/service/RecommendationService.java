package com.langtou.content.service;

import com.langtou.content.dto.NoteFeedVO;

import java.util.List;

/**
 * 个性化推荐服务接口
 * 包含召回层、排序层、冷启动策略及实时反馈机制
 */
public interface RecommendationService {

    /**
     * 个性化推荐Feed流（召回层 + 排序层）
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 推荐笔记列表
     */
    List<NoteFeedVO> recommendFeed(Long userId, int page, int size);

    /**
     * 冷启动推荐（新用户/无画像用户）
     * 返回热门内容 + 编辑精选，保底曝光
     *
     * @param page 页码
     * @param size 每页数量
     * @return 推荐笔记列表
     */
    List<NoteFeedVO> recommendForNewUser(int page, int size);

    /**
     * 记录用户行为（实时反馈）
     * 行为数据写入 Redis，定时任务刷新用户画像
     *
     * @param userId     用户ID
     * @param noteId     笔记ID
     * @param actionType 行为类型：view/like/comment/collect/share
     */
    void recordUserAction(Long userId, Long noteId, String actionType);

    /**
     * 刷新用户画像
     * 基于历史行为和实时反馈重新计算兴趣标签权重
     *
     * @param userId 用户ID
     */
    void refreshUserProfile(Long userId);
}
