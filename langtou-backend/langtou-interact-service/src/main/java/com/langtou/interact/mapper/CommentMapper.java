package com.langtou.interact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.interact.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT * FROM comment WHERE content_id = #{contentId} AND parent_id IS NULL AND deleted = 0 ORDER BY create_time DESC")
    List<Comment> selectRootCommentsByContentId(@Param("contentId") Long contentId);

    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY create_time ASC")
    List<Comment> selectRepliesByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM comment WHERE content_id = #{contentId} AND deleted = 0")
    Long countByContentId(@Param("contentId") Long contentId);
}
