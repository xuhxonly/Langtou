package com.langtou.creator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.creator.entity.CreatorDailyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CreatorDailyStatsMapper extends BaseMapper<CreatorDailyStats> {

    @Select("SELECT * FROM creator_daily_stats WHERE creator_id = #{creatorId} AND date >= #{startDate} AND date <= #{endDate} ORDER BY date ASC")
    List<CreatorDailyStats> selectByCreatorIdAndDateRange(@Param("creatorId") Long creatorId,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);
}