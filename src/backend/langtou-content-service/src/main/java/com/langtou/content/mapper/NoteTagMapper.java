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

    /**
     * 批量查询多个笔记关联的标签ID列表（避免N+1查询）
     * 返回 Map 结构：noteId -> tagId列表
     */
    @Select("<script>" +
            "SELECT note_id, tag_id FROM note_tag WHERE note_id IN " +
            "<foreach collection='noteIds' item='noteId' open='(' separator=',' close=')'>" +
            "#{noteId}" +
            "</foreach>" +
            " AND deleted = 0" +
            "</script>")
    List<NoteTag> selectByNoteIds(@Param("noteIds") List<Long> noteIds);
}
