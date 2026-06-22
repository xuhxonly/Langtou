package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.Activity;
import com.langtou.content.entity.ActivityParticipant;
import com.langtou.content.entity.ActivityTag;
import com.langtou.content.mapper.ActivityMapper;
import com.langtou.content.mapper.ActivityParticipantMapper;
import com.langtou.content.mapper.ActivityTagMapper;
import com.langtou.content.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityParticipantMapper activityParticipantMapper;
    private final ActivityTagMapper activityTagMapper;

    @Override
    @Transactional
    public Activity createActivity(Activity activity, List<String> tagNames) {
        // 参数校验
        if (!StringUtils.hasText(activity.getTitle())) {
            throw new BusinessException("活动标题不能为空");
        }
        if (!StringUtils.hasText(activity.getDescription())) {
            throw new BusinessException("活动描述不能为空");
        }
        if (!StringUtils.hasText(activity.getCoverUrl())) {
            throw new BusinessException("活动封面不能为空");
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (activity.getStartTime().isAfter(activity.getEndTime())) {
            throw new BusinessException("活动开始时间不能晚于结束时间");
        }
        if (activity.getType() == null) {
            activity.setType("CHALLENGE");
        }
        if (activity.getStatus() == null) {
            activity.setStatus("DRAFT");
        }
        if (activity.getParticipantCount() == null) {
            activity.setParticipantCount(0);
        }
        if (activity.getNoteCount() == null) {
            activity.setNoteCount(0);
        }
        if (activity.getTotalViews() == null) {
            activity.setTotalViews(0L);
        }
        if (activity.getTotalInteractions() == null) {
            activity.setTotalInteractions(0L);
        }

        activityMapper.insert(activity);

        // 保存标签
        saveTags(activity.getId(), tagNames);

        log.info("创建活动成功: id={}, title={}", activity.getId(), activity.getTitle());
        return activity;
    }

    @Override
    @Transactional
    public Activity updateActivity(Long activityId, Activity activity, List<String> tagNames) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        // 已上线活动仅允许修改非核心信息
        if ("ONLINE".equals(existing.getStatus())) {
            activity.setType(null);
            activity.setStartTime(null);
            activity.setParticipationRules(null);
            activity.setRewardConfig(null);
        }

        activity.setId(activityId);
        // 保护统计字段不被覆盖
        activity.setParticipantCount(null);
        activity.setNoteCount(null);
        activity.setTotalViews(null);
        activity.setTotalInteractions(null);
        activity.setStatus(null);
        activity.setCreatorId(null);

        activityMapper.updateById(activity);

        // 更新标签
        if (tagNames != null) {
            // 删除旧标签
            activityTagMapper.delete(
                    new QueryWrapper<ActivityTag>().eq("activity_id", activityId)
            );
            // 保存新标签
            saveTags(activityId, tagNames);
        }

        log.info("更新活动成功: id={}", activityId);
        return activityMapper.selectById(activityId);
    }

    @Override
    @Transactional
    public void deleteActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }
        // 仅草稿和已驳回状态可删除
        if (!"DRAFT".equals(existing.getStatus()) && !"REJECTED".equals(existing.getStatus())) {
            throw new BusinessException("当前状态不允许删除，仅草稿和已驳回状态可删除");
        }

        // 删除关联标签
        activityTagMapper.delete(
                new QueryWrapper<ActivityTag>().eq("activity_id", activityId)
        );
        // 删除参与记录
        activityParticipantMapper.delete(
                new QueryWrapper<ActivityParticipant>().eq("activity_id", activityId)
        );
        // 删除活动
        activityMapper.deleteById(activityId);

        log.info("删除活动成功: id={}", activityId);
    }

    @Override
    @Transactional
    public Activity publishActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        String currentStatus = existing.getStatus();
        if ("DRAFT".equals(currentStatus)) {
            existing.setStatus("PENDING_REVIEW");
        } else if ("PENDING_REVIEW".equals(currentStatus)) {
            existing.setStatus("ONLINE");
        } else {
            throw new BusinessException("当前状态不允许发布: " + currentStatus);
        }

        activityMapper.updateById(existing);
        log.info("发布活动成功: id={}, status={}", activityId, existing.getStatus());
        return existing;
    }

    @Override
    @Transactional
    public Activity endActivity(Long activityId) {
        Activity existing = activityMapper.selectById(activityId);
        if (existing == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }
        if (!"ONLINE".equals(existing.getStatus())) {
            throw new BusinessException("仅在线活动可以结束");
        }

        existing.setStatus("ENDED");
        activityMapper.updateById(existing);

        log.info("结束活动成功: id={}", activityId);
        return existing;
    }

    @Override
    @Transactional
    public void joinActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }
        if (!"ONLINE".equals(activity.getStatus())) {
            throw new BusinessException("活动不在进行中，无法参与");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("不在活动时间范围内");
        }

        // 检查是否已参与
        int exists = activityParticipantMapper.existsByActivityAndUser(activityId, userId);
        if (exists > 0) {
            throw new BusinessException("已参与该活动，请勿重复参与");
        }

        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activityId);
        participant.setUserId(userId);
        participant.setNoteCount(0);
        participant.setJoinedAt(now);
        activityParticipantMapper.insert(participant);

        // 更新活动参与人数
        activityMapper.incrementParticipantCount(activityId);

        log.info("用户参与活动: activityId={}, userId={}", activityId, userId);
    }

    @Override
    @Transactional
    public void quitActivity(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        int exists = activityParticipantMapper.existsByActivityAndUser(activityId, userId);
        if (exists == 0) {
            throw new BusinessException("未参与该活动");
        }

        activityParticipantMapper.delete(
                new QueryWrapper<ActivityParticipant>()
                        .eq("activity_id", activityId)
                        .eq("user_id", userId)
        );

        // 更新活动参与人数
        activityMapper.decrementParticipantCount(activityId);

        log.info("用户退出活动: activityId={}, userId={}", activityId, userId);
    }

    @Override
    public PageResult<Activity> listOnlineActivities(Integer page, Integer size, String type) {
        Page<Activity> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 10);
        QueryWrapper<Activity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ONLINE");

        if (StringUtils.hasText(type)) {
            wrapper.eq("type", type);
        }

        // 仅展示在时间范围内的活动
        LocalDateTime now = LocalDateTime.now();
        wrapper.le("start_time", now).ge("end_time", now);

        wrapper.orderByDesc("created_at");

        Page<Activity> resultPage = activityMapper.selectPage(pageParam, wrapper);
        return PageResult.of(resultPage);
    }

    @Override
    public PageResult<Activity> listAllActivities(Integer page, Integer size, String status, String type, String keyword) {
        Page<Activity> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        QueryWrapper<Activity> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq("type", type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like("title", keyword);
        }

        wrapper.orderByDesc("created_at");

        Page<Activity> resultPage = activityMapper.selectPage(pageParam, wrapper);
        return PageResult.of(resultPage);
    }

    @Override
    public Activity getActivityDetail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        // 增加浏览量
        activityMapper.incrementViewCount(activityId);

        return activity;
    }

    @Override
    public List<ActivityParticipant> getActivityRanking(Long activityId, String sortBy, Integer limit) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        QueryWrapper<ActivityParticipant> wrapper = new QueryWrapper<>();
        wrapper.eq("activity_id", activityId);

        if ("note_count".equals(sortBy)) {
            wrapper.orderByDesc("note_count");
        } else {
            // 默认按参与时间排序
            wrapper.orderByDesc("joined_at");
        }

        int queryLimit = limit != null ? limit : 50;
        wrapper.last("LIMIT " + queryLimit);

        return activityParticipantMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getActivityStats(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在: " + activityId);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("activityId", activityId);
        stats.put("title", activity.getTitle());
        stats.put("status", activity.getStatus());
        stats.put("participantCount", activity.getParticipantCount());
        stats.put("noteCount", activity.getNoteCount());
        stats.put("totalViews", activity.getTotalViews());
        stats.put("totalInteractions", activity.getTotalInteractions());

        // 计算平均互动率
        double avgInteractionRate = 0.0;
        if (activity.getNoteCount() != null && activity.getNoteCount() > 0) {
            avgInteractionRate = (double) activity.getTotalInteractions() / activity.getNoteCount();
        }
        stats.put("avgInteractionRate", Math.round(avgInteractionRate * 100.0) / 100.0);

        return stats;
    }

    /**
     * 保存活动标签
     */
    private void saveTags(Long activityId, List<String> tagNames) {
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }
        for (String tagName : tagNames) {
            if (StringUtils.hasText(tagName)) {
                ActivityTag tag = new ActivityTag();
                tag.setActivityId(activityId);
                tag.setTagName(tagName.trim());
                activityTagMapper.insert(tag);
            }
        }
    }
}
