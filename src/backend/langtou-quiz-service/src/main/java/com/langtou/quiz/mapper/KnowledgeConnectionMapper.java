package com.langtou.quiz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.quiz.entity.KnowledgeConnection;
import com.langtou.quiz.enums.ConnectionTypeEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeConnectionMapper extends BaseMapper<KnowledgeConnection> {

    @Select("SELECT * FROM knowledge_connection WHERE source_question_id = #{sourceId}")
    List<KnowledgeConnection> findBySourceQuestionId(@Param("sourceId") Long sourceId);

    @Select("SELECT * FROM knowledge_connection WHERE topic = #{topic}")
    List<KnowledgeConnection> findByTopic(@Param("topic") String topic);

    @Select("SELECT * FROM knowledge_connection WHERE connection_type = #{type} LIMIT #{limit}")
    List<KnowledgeConnection> findByType(@Param("type") ConnectionTypeEnum type, @Param("limit") int limit);
}
