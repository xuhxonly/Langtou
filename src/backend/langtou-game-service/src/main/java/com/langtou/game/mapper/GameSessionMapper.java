﻿﻿﻿﻿﻿package com.langtou.game.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.langtou.game.entity.GameSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GameSessionMapper extends BaseMapper<GameSession> {

    @Update("UPDATE game_session SET current_players = current_players + 1 WHERE id = #{id} AND status = 'WAITING' AND current_players < max_players")
    int incrementCurrentPlayersIfWaiting(@Param("id") Long id);

    @Update("UPDATE game_session SET current_players = GREATEST(0, current_players - 1) WHERE id = #{id}")
    int decrementCurrentPlayers(@Param("id") Long id);
}