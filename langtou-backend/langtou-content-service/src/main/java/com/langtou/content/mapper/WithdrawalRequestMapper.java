package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.WithdrawalRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WithdrawalRequestMapper extends BaseMapper<WithdrawalRequest> {

    /**
     * 按创作者查询提现记录
     */
    @Select("SELECT * FROM withdrawal_requests WHERE creator_id = #{creatorId} ORDER BY requested_at DESC")
    List<WithdrawalRequest> selectByCreatorId(@Param("creatorId") Long creatorId);

    /**
     * 按状态查询提现记录（管理员用）
     */
    @Select("SELECT * FROM withdrawal_requests WHERE status = #{status} ORDER BY requested_at DESC")
    List<WithdrawalRequest> selectByStatus(@Param("status") String status);
}
