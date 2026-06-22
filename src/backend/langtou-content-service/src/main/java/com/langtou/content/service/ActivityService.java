package com.langtou.content.service;

import com.langtou.common.result.PageResult;
import com.langtou.content.entity.Activity;
import com.langtou.content.entity.ActivityParticipant;

import java.util.List;
import java.util.Map;

/**
 * 活动服务接口
 */
public interface ActivityService {

    /**
     * 创建活动
     */
    Activity createActivity(Activity activity, List<String> tagNames);

    /**
     * 更新活动
     */
    Activity updateActivity(Long activityId, Activity activity, List<String> tagNames);

    /**
     * 删除活动（仅草稿和已驳回状态可删除）
     */
    void deleteActivity(Long activityId);

    /**
     * 发布活动（DRAFT -> PENDING_REVIEW -> ONLINE）
     */
    Activity publishActivity(Long activityId);

    /**
     * 结束活动
     */
    Activity endActivity(Long activityId);

    /**
     * 用户参与活动
     */
    void joinActivity(Long activityId, Long userId);

    /**
     * 用户退出活动
     */
    void quitActivity(Long activityId, Long userId);

    /**
     * 活动列表（用户端：在线活动）
     */
    PageResult<Activity> listOnlineActivities(Integer page, Integer size, String type);

    /**
     * 活动列表（管理端：全状态）
     */
    PageResult<Activity> listAllActivities(Integer page, Integer size, String status, String type, String keyword);

    /**
     * 活动详情
     */
    Activity getActivityDetail(Long activityId);

    /**
     * 活动排行榜（按参与笔记数排序）
     */
    List<ActivityParticipant> getActivityRanking(Long activityId, String sortBy, Integer limit);

    /**
     * 活动统计数据
     */
    Map<String, Object> getActivityStats(Long activityId);
}
