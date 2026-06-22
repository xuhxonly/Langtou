package com.langtou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.user.entity.UserAchievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户成就Mapper
 */
@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {

    @Select("SELECT * FROM user_achievement WHERE user_id = #{userId} ORDER BY obtained_at DESC")
    List<UserAchievement> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_achievement WHERE user_id = #{userId} AND achievement_type = #{type}")
    int countByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);
}
