package com.langtou.interact.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.interact.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    @Select("SELECT * FROM like_record WHERE user_id = #{userId} AND content_id = #{contentId} AND deleted = 0 LIMIT 1")
    LikeRecord selectByUserAndContent(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Select("SELECT COUNT(*) FROM like_record WHERE content_id = #{contentId} AND deleted = 0")
    Long countByContentId(@Param("contentId") Long contentId);
}
