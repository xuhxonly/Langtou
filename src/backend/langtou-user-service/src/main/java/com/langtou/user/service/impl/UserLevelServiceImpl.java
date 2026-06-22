package com.langtou.user.service.impl;

import com.langtou.user.dto.UserAchievementDTO;
import com.langtou.user.dto.UserLevelDTO;
import com.langtou.user.entity.PointsRecord;
import com.langtou.user.entity.UserAchievement;
import com.langtou.user.entity.UserLevel;
import com.langtou.user.mapper.PointsRecordMapper;
import com.langtou.user.mapper.UserAchievementMapper;
import com.langtou.user.mapper.UserLevelMapper;
import com.langtou.user.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户成长体系服务实现
 *
 * 积分规则：
 * - 发布笔记 +10分
 * - 获得点赞 +1分
 * - 获得关注 +5分
 * - 评论 +2分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLevelServiceImpl implements UserLevelService {

    private final UserLevelMapper userLevelMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final PointsRecordMapper pointsRecordMapper;

    // 等级经验阈值表
    private static final int[] LEVEL_EXPERIENCE_THRESHOLDS = {
            0,       // Level 1
            100,     // Level 2
            300,     // Level 3
            600,     // Level 4
            1000,    // Level 5
            1500,    // Level 6
            2100,    // Level 7
            2800,    // Level 8
            3600,    // Level 9
            4500,    // Level 10
    };

    @Override
    public UserLevelDTO getUserLevel(Long userId) {
        UserLevel userLevel = userLevelMapper.selectByUserId(userId);
        if (userLevel == null) {
            // 初始化用户等级数据
            userLevel = initUserLevel(userId);
        }
        return convertToDTO(userLevel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, String actionType, int points, String description) {
        UserLevel userLevel = userLevelMapper.selectByUserId(userId);
        if (userLevel == null) {
            userLevel = initUserLevel(userId);
        }

        // 更新积分
        int experience = Math.abs(points);
        userLevelMapper.addPoints(userId, points, experience);

        // 记录积分流水
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setActionType(actionType);
        record.setPoints(points);
        record.setDescription(description);
        pointsRecordMapper.insert(record);

        log.info("用户积分增加: userId={}, actionType={}, points={}, description={}",
                userId, actionType, points, description);

        // 检查等级升级
        updateUserLevel(userId);
    }

    @Override
    public void updateUserLevel(Long userId) {
        UserLevel userLevel = userLevelMapper.selectByUserId(userId);
        if (userLevel == null) {
            return;
        }

        int currentExp = userLevel.getExperience();
        int newLevel = calculateLevel(currentExp);

        if (newLevel > userLevel.getLevel()) {
            userLevelMapper.updateLevel(userId, newLevel);
            log.info("用户等级提升: userId={}, oldLevel={}, newLevel={}",
                    userId, userLevel.getLevel(), newLevel);

            // 授予等级成就
            grantAchievement(userId, "LEVEL_UP_" + newLevel,
                    "等级达到 Lv." + newLevel,
                    "恭喜您升级到 Lv." + newLevel);
        }
    }

    @Override
    public List<UserAchievementDTO> getUserAchievements(Long userId) {
        List<UserAchievement> achievements = userAchievementMapper.selectByUserId(userId);
        return achievements.stream()
                .map(this::convertAchievementToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantAchievement(Long userId, String achievementType, String achievementName, String description) {
        // 检查是否已获得该成就
        int count = userAchievementMapper.countByUserIdAndType(userId, achievementType);
        if (count > 0) {
            return;
        }

        UserAchievement achievement = new UserAchievement();
        achievement.setUserId(userId);
        achievement.setAchievementType(achievementType);
        achievement.setAchievementName(achievementName);
        achievement.setDescription(description);
        userAchievementMapper.insert(achievement);

        log.info("授予用户成就: userId={}, achievementType={}, achievementName={}",
                userId, achievementType, achievementName);
    }

    @Override
    public List<PointsRecord> getPointsRecords(Long userId, int limit) {
        return pointsRecordMapper.selectRecentByUserId(userId, limit);
    }

    @Override
    public void onPublishNote(Long userId) {
        addPoints(userId, "PUBLISH_NOTE", 10, "发布笔记奖励");

        // 检查首次发布成就
        int publishCount = pointsRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsRecord>()
                        .eq("user_id", userId)
                        .eq("action_type", "PUBLISH_NOTE")
        );

        if (publishCount == 1) {
            grantAchievement(userId, "FIRST_NOTE", "初出茅庐", "发布第一篇笔记");
        } else if (publishCount == 10) {
            grantAchievement(userId, "TEN_NOTES", "笔耕不辍", "发布10篇笔记");
        } else if (publishCount == 100) {
            grantAchievement(userId, "HUNDRED_NOTES", "创作达人", "发布100篇笔记");
        }
    }

    @Override
    public void onReceiveLike(Long userId) {
        addPoints(userId, "LIKE_RECEIVED", 1, "获得点赞奖励");
    }

    @Override
    public void onReceiveFollow(Long userId) {
        addPoints(userId, "FOLLOW_RECEIVED", 5, "获得关注奖励");

        // 检查粉丝数成就
        int followCount = pointsRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsRecord>()
                        .eq("user_id", userId)
                        .eq("action_type", "FOLLOW_RECEIVED")
        );

        if (followCount == 1) {
            grantAchievement(userId, "FIRST_FOLLOWER", "初露锋芒", "获得第一个粉丝");
        } else if (followCount == 100) {
            grantAchievement(userId, "HUNDRED_FOLLOWERS", "小有名气", "获得100个粉丝");
        } else if (followCount == 1000) {
            grantAchievement(userId, "THOUSAND_FOLLOWERS", "人气博主", "获得1000个粉丝");
        }
    }

    @Override
    public void onComment(Long userId) {
        addPoints(userId, "COMMENT", 2, "发表评论奖励");
    }

    @Override
    public void onReceiveComment(Long userId) {
        addPoints(userId, "COMMENT_RECEIVED", 2, "笔记获得评论奖励");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int checkIn(Long userId) {
        // 检查今日是否已签到
        String today = java.time.LocalDate.now().toString();
        int count = pointsRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsRecord>()
                        .eq("user_id", userId)
                        .eq("action_type", "CHECKIN")
                        .like("description", today)
        );

        if (count > 0) {
            log.info("用户今日已签到: userId={}", userId);
            return 0;
        }

        // 发放签到积分
        addPoints(userId, "CHECKIN", 3, "每日签到奖励 (" + today + ")");

        // 检查连续签到成就
        int checkinCount = pointsRecordMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsRecord>()
                        .eq("user_id", userId)
                        .eq("action_type", "CHECKIN")
        );

        if (checkinCount == 7) {
            grantAchievement(userId, "WEEK_CHECKIN", "坚持一周", "连续签到7天");
        } else if (checkinCount == 30) {
            grantAchievement(userId, "MONTH_CHECKIN", "持之以恒", "连续签到30天");
        }

        log.info("用户签到成功: userId={}, points=3", userId);
        return 3;
    }

    /**
     * 初始化用户等级数据
     */
    private UserLevel initUserLevel(Long userId) {
        UserLevel userLevel = new UserLevel();
        userLevel.setUserId(userId);
        userLevel.setLevel(1);
        userLevel.setPoints(0);
        userLevel.setExperience(0);
        userLevel.setTotalPoints(0);
        userLevelMapper.insert(userLevel);
        log.info("初始化用户等级数据: userId={}", userId);
        return userLevel;
    }

    /**
     * 根据经验值计算等级
     */
    private int calculateLevel(int experience) {
        int level = 1;
        for (int i = 1; i < LEVEL_EXPERIENCE_THRESHOLDS.length; i++) {
            if (experience >= LEVEL_EXPERIENCE_THRESHOLDS[i]) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    /**
     * 计算下一级所需经验
     */
    private int getNextLevelExperience(int currentLevel) {
        if (currentLevel < LEVEL_EXPERIENCE_THRESHOLDS.length) {
            return LEVEL_EXPERIENCE_THRESHOLDS[currentLevel];
        }
        return LEVEL_EXPERIENCE_THRESHOLDS[LEVEL_EXPERIENCE_THRESHOLDS.length - 1] * 2;
    }

    private UserLevelDTO convertToDTO(UserLevel userLevel) {
        UserLevelDTO dto = new UserLevelDTO();
        BeanUtils.copyProperties(userLevel, dto);
        dto.setNextLevelExperience(getNextLevelExperience(userLevel.getLevel()));
        return dto;
    }

    private UserAchievementDTO convertAchievementToDTO(UserAchievement achievement) {
        UserAchievementDTO dto = new UserAchievementDTO();
        BeanUtils.copyProperties(achievement, dto);
        return dto;
    }
}
