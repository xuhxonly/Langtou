package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.NoteTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoteTagMapper extends BaseMapper<NoteTag> {

    /**
     * 根据笔记ID查询关联的标签ID列表
     */
    @Select("SELECT tag_id FROM note_tag WHERE note_id = #{noteId} AND deleted = 0")
    List<Long> selectTagIdsByNoteId(@Param("noteId") Long noteId);

    /**
     * 根据标签ID查询关联的笔记ID列表
     */
    @Select("SELECT note_id FROM note_tag WHERE tag_id = #{tagId} AND deleted = 0")
    List<Long> selectNoteIdsByTagId(@Param("tagId") Long tagId);
}
