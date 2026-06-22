package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.FraudReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FraudReportMapper extends BaseMapper<FraudReport> {

    /**
     * 处理举报
     */
    @Update("UPDATE fraud_reports SET status = #{status}, processed_at = NOW(), processor_id = #{processorId} WHERE id = #{id} AND status = 'PENDING'")
    int processReport(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("processorId") Long processorId);
}
