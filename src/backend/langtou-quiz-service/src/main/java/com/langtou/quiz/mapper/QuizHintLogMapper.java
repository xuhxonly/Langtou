package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.QuizHintLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuizHintLogMapper extends BaseMapper<QuizHintLog> {

    @Select("SELECT * FROM quiz_hint_log WHERE user_id = #{userId} AND question_id = #{questionId} ORDER BY hint_level DESC, id DESC LIMIT 1")
    QuizHintLog findLatestByUserAndQuestion(@Param("userId") Long userId,
                                           @Param("questionId") Long questionId);

    @Select("SELECT * FROM quiz_hint_log WHERE user_id = #{userId} AND quiz_set_id = #{quizSetId} ORDER BY id DESC")
    List<QuizHintLog> findByUserAndQuizSet(@Param("userId") Long userId,
                                           @Param("quizSetId") Long quizSetId);
}
