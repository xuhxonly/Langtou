package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.QuizQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuizQuestionMapper extends BaseMapper<QuizQuestion> {

    @Select("SELECT * FROM quiz_question WHERE quiz_set_id = #{quizSetId} ORDER BY sequence_no ASC")
    List<QuizQuestion> listByQuizSetId(@Param("quizSetId") Long quizSetId);
}
