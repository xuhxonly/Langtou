package com.langtou.user.service;

import com.langtou.common.result.PageResult;
import com.langtou.user.dto.UserProfileVO;

import java.util.List;

public interface FollowService {

    /**
     * 关注用户
     */
    void follow(Long followerId, Long followingId);

    /**
     * 取消关注
     */
    void unfollow(Long followerId, Long followingId);

    /**
     * 获取粉丝列表（分页）
     */
    PageResult<UserProfileVO> getFollowers(Long userId, int page, int size);

    /**
     * 获取关注列表（分页）
     */
    PageResult<UserProfileVO> getFollowing(Long userId, int page, int size);

    /**
     * 查询与当前用户的关系
     * @return 0=无关系, 1=已关注对方, 2=互相关注
     */
    int getRelationship(Long currentUserId, Long targetUserId);

    /**
     * 获取用户粉丝数
     */
    long getFollowerCount(Long userId);

    /**
     * 获取用户关注数
     */
    long getFollowingCount(Long userId);

    /**
     * 用户搜索（按用户名或昵称模糊匹配）
     */
    PageResult<UserProfileVO> searchUsers(String keyword, int page, int size);

    /**
     * 获取用户关注的所有用户ID
     */
    List<Long> getFollowingIds(Long userId);
}
