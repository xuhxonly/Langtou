package com.langtou.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.client.NotificationClient;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.common.result.ResultCode;
import com.langtou.common.utils.RedisKeyUtil;
import com.langtou.user.dto.UserProfileVO;
import com.langtou.user.entity.Follow;
import com.langtou.user.entity.User;
import com.langtou.user.mapper.FollowMapper;
import com.langtou.user.mapper.UserMapper;
import com.langtou.user.service.FollowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ResultCode.FOLLOW_SELF);
        }

        User targetUser = userMapper.selectById(followingId);
        if (targetUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Follow existFollow = followMapper.selectByFollowerAndFollowing(followerId, followingId);
        if (existFollow != null) {
            throw new BusinessException(ResultCode.FOLLOW_ALREADY);
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        followMapper.insert(follow);
        // 更新双方计数
        User follower = userMapper.selectById(followerId);
        User following = userMapper.selectById(followingId);
        if (follower != null) {
            follower.setFollowingCount(follower.getFollowingCount() + 1);
            userMapper.updateById(follower);
        }
        if (following != null) {
            following.setFollowerCount(following.getFollowerCount() + 1);
            userMapper.updateById(following);
        }
        // 发送关注通知
        sendFollowNotification(followingId, followerId);
        log.info("关注成功: followerId={}, followingId={}", followerId, followingId);
    }

    private void sendFollowNotification(Long targetUserId, Long fromUserId) {
        try {
            if (targetUserId.equals(fromUserId)) {
                return;
            }
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", targetUserId);
            notification.put("type", "FOLLOW");
            notification.put("sourceId", fromUserId);
            notification.put("sourceType", "user");
            notification.put("content", "关注了你");
            notification.put("fromUserId", fromUserId);
            notificationClient.createNotification(notification);
        } catch (Exception e) {
            log.warn("发送关注通知失败: targetUserId={}, error={}", targetUserId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followMapper.selectByFollowerAndFollowing(followerId, followingId);
        if (follow == null) {
            throw new BusinessException(ResultCode.FOLLOW_NOT_YET);
        }
        followMapper.deleteById(follow.getId());
        // 更新双方计数
        User follower = userMapper.selectById(followerId);
        User following = userMapper.selectById(followingId);
        if (follower != null && follower.getFollowingCount() > 0) {
            follower.setFollowingCount(follower.getFollowingCount() - 1);
            userMapper.updateById(follower);
        }
        if (following != null && following.getFollowerCount() > 0) {
            following.setFollowerCount(following.getFollowerCount() - 1);
            userMapper.updateById(following);
        }
        log.info("取消关注成功: followerId={}, followingId={}", followerId, followingId);
    }

    @Override
    public PageResult<UserProfileVO> getFollowers(Long userId, int page, int size) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.followersKey(userId, page, size);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return objectMapper.readValue(cached, PageResult.class);
            }
        } catch (Exception e) {
            log.warn("读取粉丝列表缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        Page<Follow> followPage = new Page<>(page, size);
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("following_id", userId)
                .orderByDesc("created_at");
        Page<Follow> result = followMapper.selectPage(followPage, wrapper);

        List<Long> followerIds = result.getRecords().stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());

        if (followerIds.isEmpty()) {
            return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), Collections.emptyList());
        }

        List<User> users = userMapper.selectBatchIds(followerIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<UserProfileVO> records = result.getRecords().stream()
                .map(follow -> {
                    User user = userMap.get(follow.getFollowerId());
                    if (user == null) {
                        return null;
                    }
                    return convertToProfileVO(user);
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        PageResult<UserProfileVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);

        // 写入Redis缓存（TTL 10分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(pageResult),
                    Duration.ofSeconds(RedisKeyUtil.FOLLOW_LIST_TTL));
        } catch (Exception e) {
            log.warn("写入粉丝列表缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return pageResult;
    }

    @Override
    public PageResult<UserProfileVO> getFollowing(Long userId, int page, int size) {
        // 先查Redis缓存
        String cacheKey = RedisKeyUtil.followingKey(userId, page, size);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                return objectMapper.readValue(cached, PageResult.class);
            }
        } catch (Exception e) {
            log.warn("读取关注列表缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        Page<Follow> followPage = new Page<>(page, size);
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", userId)
                .orderByDesc("created_at");
        Page<Follow> result = followMapper.selectPage(followPage, wrapper);

        List<Long> followingIds = result.getRecords().stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), Collections.emptyList());
        }

        List<User> users = userMapper.selectBatchIds(followingIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<UserProfileVO> records = result.getRecords().stream()
                .map(follow -> {
                    User user = userMap.get(follow.getFollowingId());
                    if (user == null) {
                        return null;
                    }
                    return convertToProfileVO(user);
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        PageResult<UserProfileVO> pageResult = PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);

        // 写入Redis缓存（TTL 10分钟）
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(pageResult),
                    Duration.ofSeconds(RedisKeyUtil.FOLLOW_LIST_TTL));
        } catch (Exception e) {
            log.warn("写入关注列表缓存失败: userId={}, error={}", userId, e.getMessage());
        }

        return pageResult;
    }

    @Override
    public int getRelationship(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null) {
            return 0;
        }
        if (currentUserId.equals(targetUserId)) {
            return 0;
        }

        Follow currentFollowTarget = followMapper.selectByFollowerAndFollowing(currentUserId, targetUserId);
        Follow targetFollowCurrent = followMapper.selectByFollowerAndFollowing(targetUserId, currentUserId);

        if (currentFollowTarget != null && targetFollowCurrent != null) {
            return 2;
        } else if (currentFollowTarget != null) {
            return 1;
        }
        return 0;
    }

    @Override
    public long getFollowerCount(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("following_id", userId);
        return followMapper.selectCount(wrapper);
    }

    @Override
    public long getFollowingCount(Long userId) {
        QueryWrapper<Follow> wrapper = new QueryWrapper<>();
        wrapper.eq("follower_id", userId);
        return followMapper.selectCount(wrapper);
    }

    private UserProfileVO convertToProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatarUrl());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setFollowingCount(user.getFollowingCount());
        return vo;
    }

    @Override
    public PageResult<UserProfileVO> searchUsers(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return PageResult.of(0L, (long) page, (long) size, List.of());
        }

        Page<User> userPage = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.like("username", keyword)
                .or()
                .like("nickname", keyword)
                .orderByDesc("created_at");
        Page<User> result = userMapper.selectPage(userPage, wrapper);

        List<UserProfileVO> records = result.getRecords().stream()
                .map(this::convertToProfileVO)
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public List<Long> getFollowingIds(Long userId) {
        return followMapper.selectFollowingIdsByFollowerId(userId);
    }
}
