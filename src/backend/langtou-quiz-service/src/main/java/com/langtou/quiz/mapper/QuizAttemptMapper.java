package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.QuizAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizAttemptMapper extends BaseMapper<QuizAttempt> {

    @Select("SELECT * FROM quiz_attempt WHERE game_session_id = #{gameSessionId} AND status = 'IN_PROGRESS' LIMIT 1")
    QuizAttempt findByGameSessionId(@Param("gameSessionId") Long gameSessionId);
}