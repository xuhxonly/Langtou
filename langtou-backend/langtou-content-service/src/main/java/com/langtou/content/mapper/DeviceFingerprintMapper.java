package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.DeviceFingerprint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeviceFingerprintMapper extends BaseMapper<DeviceFingerprint> {

    /**
     * 根据设备ID查询指纹记录
     */
    @Select("SELECT * FROM device_fingerprints WHERE device_id = #{deviceId}")
    DeviceFingerprint selectByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 封禁设备
     */
    @Update("UPDATE device_fingerprints SET is_blocked = 1 WHERE device_id = #{deviceId}")
    int blockByDeviceId(@Param("deviceId") String deviceId);

    /**
     * 检查设备是否被封禁
     */
    @Select("SELECT is_blocked FROM device_fingerprints WHERE device_id = #{deviceId} LIMIT 1")
    Integer checkBlocked(@Param("deviceId") String deviceId);
}
