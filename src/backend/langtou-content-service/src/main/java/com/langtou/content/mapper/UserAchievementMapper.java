package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.UserAchievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {

    @Select("SELECT * FROM user_achievements WHERE user_id = #{userId} ORDER BY unlocked_at DESC")
    List<UserAchievement> selectByUserId(@Param("userId") Long userId);
}
