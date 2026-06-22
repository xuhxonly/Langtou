package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.UserSkillProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserSkillProfileMapper extends BaseMapper<UserSkillProfile> {

    @Select("SELECT * FROM user_skill_profile WHERE user_id = #{userId} AND subject = #{subject}")
    Optional<UserSkillProfile> findByUserAndSubject(@Param("userId") Long userId,
                                                     @Param("subject") String subject);
}
