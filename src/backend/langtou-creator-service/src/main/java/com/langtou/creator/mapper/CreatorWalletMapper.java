package com.langtou.creator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.creator.entity.CreatorWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CreatorWalletMapper extends BaseMapper<CreatorWallet> {

    @Select("SELECT * FROM creator_wallet WHERE creator_id = #{creatorId}")
    CreatorWallet selectByCreatorId(@Param("creatorId") Long creatorId);
}
