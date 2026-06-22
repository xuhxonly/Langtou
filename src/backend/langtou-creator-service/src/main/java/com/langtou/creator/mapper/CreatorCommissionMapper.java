package com.langtou.creator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.creator.entity.CreatorCommission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface CreatorCommissionMapper extends BaseMapper<CreatorCommission> {

    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM creator_commissions WHERE creator_id = #{creatorId}")
    BigDecimal sumCommissionByCreator(@Param("creatorId") Long creatorId);

    @Select("SELECT COALESCE(SUM(commission_amount), 0) FROM creator_commissions WHERE creator_id = #{creatorId} AND status = #{status}")
    BigDecimal sumCommissionByCreatorAndStatus(@Param("creatorId") Long creatorId, @Param("status") String status);

    @Select("SELECT DATE(created_at) AS date, COALESCE(SUM(commission_amount), 0) AS amount " +
            "FROM creator_commissions " +
            "WHERE creator_id = #{creatorId} AND created_at >= #{startDate} AND created_at <= #{endDate} " +
            "GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> sumDailyCommission(@Param("creatorId") Long creatorId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS date, COALESCE(SUM(commission_amount), 0) AS amount " +
            "FROM creator_commissions " +
            "WHERE creator_id = #{creatorId} AND created_at >= #{startDate} AND created_at <= #{endDate} " +
            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY date")
    List<Map<String, Object>> sumMonthlyCommission(@Param("creatorId") Long creatorId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
}