package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

public interface AuditLogMapper extends BaseMapper<AuditLog> {

    /**
     * 根据图片MD5查询最近一次审核通过的记录
     */
    @Select("SELECT * FROM audit_log WHERE image_md5 = #{imageMd5} AND result = 'pass' ORDER BY created_at DESC LIMIT 1")
    AuditLog findLatestPassByImageMd5(String imageMd5);
}
