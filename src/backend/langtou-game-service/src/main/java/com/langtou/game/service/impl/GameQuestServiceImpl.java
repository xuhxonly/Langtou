﻿﻿﻿﻿﻿package com.langtou.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.common.exception.BusinessException;
import com.langtou.game.dto.GameQuestProgressRequest;
import com.langtou.game.dto.GameQuestVO;
import com.langtou.game.entity.GameInventory;
import com.langtou.game.entity.GameItem;
import com.langtou.game.entity.GameQuest;
import com.langtou.game.mapper.GameInventoryMapper;
import com.langtou.game.mapper.GameItemMapper;
import com.langtou.game.mapper.GameQuestMapper;
import com.langtou.game.service.GameQuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameQuestServiceImpl implements GameQuestService {

    private static final String QUEST_PROGRESS_KEY_PREFIX = "game:quest:progress:";
    private static final long QUEST_PROGRESS_TTL_SECONDS = 60L * 60 * 24 * 7;

    private final GameQuestMapper questMapper;
    private final GameInventoryMapper inventoryMapper;
    private final GameItemMapper itemMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<GameQuestVO> listByGameId(Long gameId) {
        LambdaQueryWrapper<GameQuest> wrapper = new LambdaQueryWrapper<GameQuest>()
                .eq(GameQuest::getGameId, gameId);
        return questMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GameQuestVO claimQuest(Long questId, Long userId) {
        GameQuest quest = questMapper.selectById(questId);
        if (quest == null) {
            throw new BusinessException("任务不存在");
        }
        if (!"ACTIVE".equals(quest.getStatus())) {
            throw new BusinessException("任务当前不可领取");
        }
        String key = buildKey(userId, questId);
        String cached = stringRedisTemplate.opsForValue().get(key);
        int current = cached == null ? 0 : Integer.parseInt(cached);
        if (current < quest.getTargetValue()) {
            throw new BusinessException("任务进度未达成");
        }
        if (quest.getRewardItemId() != null) {
            GameInventory inv = new GameInventory();
            inv.setUserId(userId);
            inv.setItemId(quest.getRewardItemId());
            GameItem item = itemMapper.selectById(quest.getRewardItemId());
            inv.setItemType(item == null ? "UNKNOWN" : item.getType());
            inv.setQuantity(1);
            inv.setEquipped(false);
            inventoryMapper.insert(inv);
        }
        quest.setStatus("CLAIMED");
        quest.setUpdatedAt(LocalDateTime.now());
        questMapper.updateById(quest);
        stringRedisTemplate.delete(key);
        return toVO(quest);
    }

    @Override
    public GameQuestVO trackProgress(Long userId, GameQuestProgressRequest request) {
        GameQuest quest = questMapper.selectById(request.getQuestId());
        if (quest == null) {
            throw new BusinessException("任务不存在");
        }
        String key = buildKey(userId, quest.getId());
        String cached = stringRedisTemplate.opsForValue().get(key);
        int current = (cached == null ? 0 : Integer.parseInt(cached)) + request.getProgressValue();
        int limited = Math.min(current, quest.getTargetValue());
        stringRedisTemplate.opsForValue().set(key, String.valueOf(limited), QUEST_PROGRESS_TTL_SECONDS, TimeUnit.SECONDS);
        return toVO(quest);
    }

    @Override
    public GameQuestVO completeQuest(Long questId, Long userId) {
        return claimQuest(questId, userId);
    }

    private String buildKey(Long userId, Long questId) {
        return QUEST_PROGRESS_KEY_PREFIX + userId + ":" + questId;
    }

    private GameQuestVO toVO(GameQuest quest) {
        GameQuestVO vo = new GameQuestVO();
        vo.setId(quest.getId());
        vo.setGameId(quest.getGameId());
        vo.setTitle(quest.getTitle());
        vo.setDescription(quest.getDescription());
        vo.setType(quest.getType());
        vo.setTargetValue(quest.getTargetValue());
        vo.setRewardPoints(quest.getRewardPoints());
        vo.setRewardItemId(quest.getRewardItemId());
        vo.setStatus(quest.getStatus());
        vo.setUpdatedAt(quest.getUpdatedAt());
        return vo;
    }
}
