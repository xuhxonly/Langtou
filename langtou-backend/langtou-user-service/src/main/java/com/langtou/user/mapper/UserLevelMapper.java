package com.langtou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.user.entity.UserLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户等级/积分Mapper
 */
@Mapper
public interface UserLevelMapper extends BaseMapper<UserLevel> {

    @Select("SELECT * FROM user_level WHERE user_id = #{userId}")
    UserLevel selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE user_level SET points = points + #{points}, total_points = total_points + #{points}, " +
            "experience = experience + #{experience}, updated_at = NOW() WHERE user_id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("points") int points, @Param("experience") int experience);

    @Update("UPDATE user_level SET level = #{level}, updated_at = NOW() WHERE user_id = #{userId}")
    int updateLevel(@Param("userId") Long userId, @Param("level") int level);
}
