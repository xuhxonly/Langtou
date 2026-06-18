package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.Draft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DraftMapper extends BaseMapper<Draft> {

    @Select("SELECT * FROM draft WHERE user_id = #{userId} ORDER BY updated_at DESC")
    List<Draft> selectByUserId(@Param("userId") Long userId);
}
