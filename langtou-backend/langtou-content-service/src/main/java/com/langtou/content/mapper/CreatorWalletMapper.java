package com.langtou.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.content.entity.CreatorWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CreatorWalletMapper extends BaseMapper<CreatorWallet> {

    /**
     * 根据创作者ID查询钱包
     */
    @Select("SELECT * FROM creator_wallet WHERE creator_id = #{creatorId}")
    CreatorWallet selectByCreatorId(@Param("creatorId") Long creatorId);
}
