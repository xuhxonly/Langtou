package com.langtou.creator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.creator.entity.WithdrawalRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WithdrawalRequestMapper extends BaseMapper<WithdrawalRequest> {

    @Select("SELECT * FROM withdrawal_requests WHERE status = #{status} ORDER BY requested_at DESC")
    List<WithdrawalRequest> selectByStatus(@Param("status") String status);
}
