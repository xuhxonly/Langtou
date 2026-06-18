package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.CreatorCommission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CreatorCommissionMapper extends BaseMapper<CreatorCommission> {

    /**
     * 按创作者查询佣金记录
     */
    @Select("SELECT * FROM creator_commissions WHERE creator_id = #{creatorId} ORDER BY created_at DESC")
    List<CreatorCommission> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 按创作者汇总佣金总额
     */
    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM creator_commissions WHERE creator_id = #{creatorId} AND status = #{status}")
    BigDecimal sumCommissionByCreatorAndStatus(@Param("creatorId") Long creatorId, @Param("status") String status);

    /**
     * 按创作者汇总佣金总额（所有状态）
     */
    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM creator_commissions WHERE creator_id = #{creatorId}")
    BigDecimal sumCommissionByCreator(@Param("creatorId") Long creatorId);

    /**
     * 按创作者汇总日收益
     */
    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(commission_amount), 0) as total " +
            "FROM creator_commissions WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date DESC")
    List<Map<String, Object>> sumDailyCommission(@Param("creatorId") Long creatorId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);

    /**
     * 按创作者汇总周收益
     */
    @Select("SELECT YEARWEEK(created_at, 1) as week, COALESCE(SUM(commission_amount), 0) as total " +
            "FROM creator_commissions WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY YEARWEEK(created_at, 1) ORDER BY week DESC")
    List<Map<String, Object>> sumWeeklyCommission(@Param("creatorId") Long creatorId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);

    /**
     * 按创作者汇总月收益
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') as month, COALESCE(SUM(commission_amount), 0) as total " +
            "FROM creator_commissions WHERE creator_id = #{creatorId} " +
            "AND created_at >= #{startDate} AND created_at < #{endDate} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month DESC")
    List<Map<String, Object>> sumMonthlyCommission(@Param("creatorId") Long creatorId,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);
}
