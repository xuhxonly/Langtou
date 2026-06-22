package com.langtou.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.game.entity.GameSessionPlayer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GameSessionPlayerMapper extends BaseMapper<GameSessionPlayer> {

    @Select("SELECT COUNT(1) FROM game_session_players WHERE session_id = #{sessionId} AND user_id = #{userId}")
    int countBySessionAndUser(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
