package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 根据标签名查询
     */
    @Select("SELECT * FROM tag WHERE name = #{name} AND deleted = 0 LIMIT 1")
    Tag selectByName(@Param("name") String name);

    /**
     * 增加标签使用次数
     */
    @Update("UPDATE tag SET use_count = use_count + 1 WHERE id = #{id}")
    int incrementUseCount(@Param("id") Long id);

    /**
     * 减少标签使用次数
     */
    @Update("UPDATE tag SET use_count = use_count - 1 WHERE id = #{id} AND use_count > 0")
    int decrementUseCount(@Param("id") Long id);
}
