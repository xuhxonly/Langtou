package com.langtou.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.message.entity.PushSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 推送设置 Mapper
 */
@Mapper
public interface PushSettingsMapper extends BaseMapper<PushSettings> {

    /**
     * 根据用户ID查询推送设置
     */
    @Select("SELECT * FROM push_settings WHERE user_id = #{userId} LIMIT 1")
    PushSettings selectByUserId(@Param("userId") Long userId);
}
