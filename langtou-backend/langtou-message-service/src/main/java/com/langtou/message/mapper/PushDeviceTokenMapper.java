package com.langtou.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.message.entity.PushDeviceToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 推送设备Token Mapper
 */
@Mapper
public interface PushDeviceTokenMapper extends BaseMapper<PushDeviceToken> {

    /**
     * 根据用户ID查询设备Token列表
     */
    @Select("SELECT * FROM push_device_tokens WHERE user_id = #{userId} ORDER BY last_active_at DESC")
    List<PushDeviceToken> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据设备Token查询
     */
    @Select("SELECT * FROM push_device_tokens WHERE device_token = #{deviceToken} LIMIT 1")
    PushDeviceToken selectByDeviceToken(@Param("deviceToken") String deviceToken);
}
