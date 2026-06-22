package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    @Select("SELECT * FROM note WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<Content> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE note SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Long id);

    @Update("UPDATE note SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE note SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Long id);

    @Update("UPDATE note SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    @Update("UPDATE note SET collect_count = collect_count + 1 WHERE id = #{id}")
    int incrementCollectCount(@Param("id") Long id);

    @Update("UPDATE note SET collect_count = collect_count - 1 WHERE id = #{id} AND collect_count > 0")
    int decrementCollectCount(@Param("id") Long id);
}
