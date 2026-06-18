package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.TrafficFunnel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrafficFunnelMapper extends BaseMapper<TrafficFunnel> {

    @Select("SELECT * FROM traffic_funnel WHERE content_id = #{contentId} ORDER BY date DESC")
    List<TrafficFunnel> selectByContentId(@Param("contentId") Long contentId);

    @Select("SELECT * FROM traffic_funnel WHERE content_id = #{contentId} AND date >= #{startDate} AND date <= #{endDate} ORDER BY date DESC")
    List<TrafficFunnel> selectByContentIdAndDateRange(@Param("contentId") Long contentId,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);
}
