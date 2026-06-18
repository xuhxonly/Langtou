package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.UserOnboardingProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserOnboardingProgressMapper extends BaseMapper<UserOnboardingProgress> {

    @Select("SELECT * FROM user_onboarding_progress WHERE user_id = #{userId}")
    List<UserOnboardingProgress> selectByUserId(@Param("userId") Long userId);
}
