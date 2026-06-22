package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.UserEmotionState;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserEmotionStateMapper extends BaseMapper<UserEmotionState> {

    @Select("SELECT * FROM user_emotion_state WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    Optional<UserEmotionState> findLatestByUserId(@Param("userId") Long userId);
}
