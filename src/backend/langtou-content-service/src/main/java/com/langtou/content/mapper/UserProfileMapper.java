package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户画像 Mapper 接口
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID查询用户画像
     *
     * @param userId 用户ID
     * @return 用户画像
     */
    @Select("SELECT * FROM user_profile WHERE user_id = #{userId} LIMIT 1")
    UserProfile selectByUserId(@Param("userId") Long userId);
}
