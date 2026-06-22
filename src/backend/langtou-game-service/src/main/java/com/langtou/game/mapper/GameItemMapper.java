package com.langtou.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.game.entity.GameItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GameItemMapper extends BaseMapper<GameItem> {
}
