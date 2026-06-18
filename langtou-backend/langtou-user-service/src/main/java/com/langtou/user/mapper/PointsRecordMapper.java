package com.langtou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.user.entity.PointsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 积分记录Mapper
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    @Select("SELECT * FROM points_record WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<PointsRecord> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
