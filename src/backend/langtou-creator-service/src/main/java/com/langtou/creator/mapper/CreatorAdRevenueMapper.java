package com.langtou.creator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.creator.entity.CreatorAdRevenue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CreatorAdRevenueMapper extends BaseMapper<CreatorAdRevenue> {

    @Select("SELECT COALESCE(SUM(revenue), 0) FROM creator_ad_revenue WHERE creator_id = #{creatorId}")
    BigDecimal sumRevenueByCreator(@Param("creatorId") Long creatorId);

    @Select("SELECT COALESCE(SUM(revenue), 0) FROM creator_ad_revenue WHERE creator_id = #{creatorId} AND settlement_status = 'UNSETTLED'")
    BigDecimal sumUnsettledRevenueByCreator(@Param("creatorId") Long creatorId);

    @Select("SELECT DATE(created_at) AS date, COALESCE(SUM(revenue), 0) AS amount " +
            "FROM creator_ad_revenue " +
            "WHERE creator_id = #{creatorId} AND created_at >= #{startDate} AND created_at <= #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> sumDailyRevenue(@Param("creatorId") Long creatorId,
                                              @Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS date, COALESCE(SUM(revenue), 0) AS amount " +
            "FROM creator_ad_revenue " +
            "WHERE creator_id = #{creatorId} AND created_at >= #{startDate} AND created_at <= #{endDate} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY date")
    List<Map<String, Object>> sumMonthlyRevenue(@Param("creatorId") Long creatorId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);
}