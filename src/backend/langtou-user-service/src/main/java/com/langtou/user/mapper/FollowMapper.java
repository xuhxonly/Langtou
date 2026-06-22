package com.langtou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.user.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 查询关注关系是否存在
     */
    @Select("SELECT * FROM follow WHERE follower_id = #{followerId} AND following_id = #{followingId} AND deleted = 0 LIMIT 1")
    Follow selectByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    /**
     * 查询用户关注的所有用户ID
     */
    @Select("SELECT following_id FROM follow WHERE follower_id = #{followerId} AND deleted = 0")
    List<Long> selectFollowingIdsByFollowerId(@Param("followerId") Long followerId);
}
