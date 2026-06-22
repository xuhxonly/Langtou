package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.ContentAnalytics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContentAnalyticsMapper extends BaseMapper<ContentAnalytics> {

    @Select("SELECT * FROM content_analytics WHERE content_id = #{contentId} ORDER BY date DESC")
    List<ContentAnalytics> selectByContentId(@Param("contentId") Long contentId);

    @Select("SELECT * FROM content_analytics WHERE content_id = #{contentId} AND date >= #{startDate} AND date <= #{endDate} ORDER BY date DESC")
    List<ContentAnalytics> selectByContentIdAndDateRange(@Param("contentId") Long contentId,
                                                         @Param("startDate") String startDate,
                                                         @Param("endDate") String endDate);
}
