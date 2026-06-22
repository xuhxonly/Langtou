package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.CreatorAdRevenue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CreatorAdRevenueMapper extends BaseMapper<CreatorAdRevenue> {

    /**
     * 按创作者查询广告收益记录
     */
    @Select("SELECT * FROM creator_ad_revenue WHERE creator_id = #{creatorId} ORDER BY created_at DESC")
    List<CreatorAdRevenue> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 按创作者汇总总收益
     */
    @Select("SELECT COALESCE(SUM(revenue), 0) FROM creator_ad_revenue WHERE creator_id = #{creatorId}")
    BigDecimal sumRevenueByCreator(@Param("creatorId") Long creatorId);

    /**
     * 按创作者汇总未结算收益
     */
    @Select("SELECT COALESCE(SUM(revenue), 0) FROM creator_ad_revenue WHERE creator_id = #{creatorId} AND settlement_status = 'UNSETTLED'")
    BigDecimal sumUnsettledRevenueByCreator(@Param("creatorId") Long creatorId);

    /**
     * 按创作者汇总日收益趋势
     */
    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(revenue), 0) as total " +
            "FROM creator_ad_revenue WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date DESC")
    List<Map<String, Object>> sumDailyRevenue(@Param("creatorId") Long creatorId,
                                               @Param("startDate") String startDate,
                                               @Param("endDate") String endDate);

    /**
     * 按创作者汇总周收益趋势
     */
    @Select("SELECT YEARWEEK(created_at, 1) as week, COALESCE(SUM(revenue), 0) as total " +
            "FROM creator_ad_revenue WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY YEARWEEK(created_at, 1) ORDER BY week DESC")
    List<Map<String, Object>> sumWeeklyRevenue(@Param("creatorId") Long creatorId,
                                                 @Param("startDate") String startDate,
                                                 @Param("endDate") String endDate);

    /**
     * 按创作者汇总月收益趋势
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COALESCE(SUM(revenue), 0) as total " +
            "FROM creator_ad_revenue WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC")
    List<Map<String, Object>> sumMonthlyRevenue(@Param("creatorId") Long creatorId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);
}
