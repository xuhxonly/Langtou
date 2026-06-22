package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.QuizSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuizSetMapper extends BaseMapper<QuizSet> {

    @Select("SELECT * FROM quiz_set WHERE note_id = #{noteId} AND status IN ('READY','PUBLISHED') ORDER BY id DESC LIMIT 1")
    QuizSet findLatestByNoteId(@Param("noteId") Long noteId);
}
