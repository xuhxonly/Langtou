package com.langtou.game.service;

import com.langtou.game.dto.GameQuestProgressRequest;
import com.langtou.game.dto.GameQuestVO;

import java.util.List;

public interface GameQuestService {

    List<GameQuestVO> listByGameId(Long gameId);

    GameQuestVO claimQuest(Long questId, Long userId);

    GameQuestVO trackProgress(Long userId, GameQuestProgressRequest request);

    GameQuestVO completeQuest(Long questId, Long userId);
}
