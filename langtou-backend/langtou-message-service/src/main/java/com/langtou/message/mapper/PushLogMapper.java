package com.langtou.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.message.entity.PushLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 推送日志 Mapper
 */
@Mapper
public interface PushLogMapper extends BaseMapper<PushLog> {

    /**
     * 根据用户ID查询推送日志（按创建时间倒序）
     */
    @Select("SELECT * FROM push_logs WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<PushLog> selectByUserId(@Param("userId") Long userId);
}
