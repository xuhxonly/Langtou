package com.langtou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.user.entity.TeenModeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 青少年模式Mapper
 */
@Mapper
public interface TeenModeMapper extends BaseMapper<TeenModeLog> {

    /**
     * 根据用户ID和日期查询使用日志
     */
    @Select("SELECT * FROM teen_mode_logs WHERE user_id = #{userId} AND usage_date = #{usageDate} LIMIT 1")
    TeenModeLog selectByUserIdAndDate(@Param("userId") Long userId, @Param("usageDate") String usageDate);

    /**
     * 更新当日使用时长
     */
    int updateDailyUsage(@Param("userId") Long userId, @Param("usageDate") String usageDate, @Param("seconds") int seconds);
}
