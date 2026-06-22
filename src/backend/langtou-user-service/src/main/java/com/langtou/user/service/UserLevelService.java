package com.langtou.user.service;

import com.langtou.user.dto.UserAchievementDTO;
import com.langtou.user.dto.UserLevelDTO;
import com.langtou.user.entity.PointsRecord;

import java.util.List;

/**
 * 用户成长体系服务
 */
public interface UserLevelService {

    /**
     * 获取用户等级/积分信息
     */
    UserLevelDTO getUserLevel(Long userId);

    /**
     * 增加积分
     * @param userId 用户ID
     * @param actionType 行为类型
     * @param points 积分值
     * @param description 描述
     */
    void addPoints(Long userId, String actionType, int points, String description);

    /**
     * 计算并更新用户等级
     */
    void updateUserLevel(Long userId);

    /**
     * 获取用户成就列表
     */
    List<UserAchievementDTO> getUserAchievements(Long userId);

    /**
     * 授予成就
     */
    void grantAchievement(Long userId, String achievementType, String achievementName, String description);

    /**
     * 获取积分记录
     */
    List<PointsRecord> getPointsRecords(Long userId, int limit);

    /**
     * 处理发布笔记积分
     */
    void onPublishNote(Long userId);

    /**
     * 处理获得点赞积分
     */
    void onReceiveLike(Long userId);

    /**
     * 处理获得关注积分
     */
    void onReceiveFollow(Long userId);

    /**
     * 处理评论积分（评论者获得积分）
     */
    void onComment(Long userId);

    /**
     * 处理获得评论积分（笔记作者获得积分）
     */
    void onReceiveComment(Long userId);

    /**
     * 每日签到
     * @return 签到获得的积分
     */
    int checkIn(Long userId);
}
