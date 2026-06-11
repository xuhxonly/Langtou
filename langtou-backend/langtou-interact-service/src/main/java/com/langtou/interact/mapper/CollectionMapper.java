package com.langtou.interact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.interact.entity.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionMapper extends BaseMapper<Collection> {

    @Select("SELECT * FROM collection WHERE user_id = #{userId} AND note_id = #{noteId} AND deleted = 0 LIMIT 1")
    Collection selectByUserAndNote(@Param("userId") Long userId, @Param("noteId") Long noteId);

    @Select("SELECT COUNT(*) FROM collection WHERE note_id = #{noteId} AND deleted = 0")
    Long countByNoteId(@Param("noteId") Long noteId);
}
