package com.langtou.game.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.langtou.game.dto.GameLeaderboardVO;
import com.langtou.game.entity.GameLeaderboard;
import com.langtou.game.mapper.GameLeaderboardMapper;
import com.langtou.game.service.GameLeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameLeaderboardServiceImpl implements GameLeaderboardService {

    private final GameLeaderboardMapper leaderboardMapper;

    @Override
    public List<GameLeaderboardVO> getLeaderboard(Long gameId, Long seasonId, int limit) {
        LambdaQueryWrapper<GameLeaderboard> wrapper = new LambdaQueryWrapper<GameLeaderboard>()
                .eq(GameLeaderboard::getGameId, gameId)
                .eq(seasonId != null, GameLeaderboard::getSeasonId, seasonId)
                .orderByDesc(GameLeaderboard::getScore)
                .last("limit " + limit);
        return leaderboardMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public GameLeaderboardVO getUserRank(Long gameId, Long userId, Long seasonId) {
        LambdaQueryWrapper<GameLeaderboard> wrapper = new LambdaQueryWrapper<GameLeaderboard>()
                .eq(GameLeaderboard::getGameId, gameId)
                .eq(GameLeaderboard::getUserId, userId)
                .eq(seasonId != null, GameLeaderboard::getSeasonId, seasonId);
        GameLeaderboard lb = leaderboardMapper.selectOne(wrapper);
        return toVO(lb);
    }

    @Override
    public void updateRank(Long gameId, Long userId, Integer score, Long seasonId) {
        LambdaQueryWrapper<GameLeaderboard> existWrapper = new LambdaQueryWrapper<GameLeaderboard>()
                .eq(GameLeaderboard::getGameId, gameId)
                .eq(GameLeaderboard::getUserId, userId)
                .eq(seasonId != null, GameLeaderboard::getSeasonId, seasonId);
        GameLeaderboard exist = leaderboardMapper.selectOne(existWrapper);
        if (exist == null) {
            GameLeaderboard lb = new GameLeaderboard();
            lb.setGameId(gameId);
            lb.setUserId(userId);
            lb.setScore(score);
            lb.setSeasonId(seasonId);
            lb.setUpdatedAt(LocalDateTime.now());
            leaderboardMapper.insert(lb);
        } else {
            LambdaUpdateWrapper<GameLeaderboard> updateWrapper = new LambdaUpdateWrapper<GameLeaderboard>()
                    .eq(GameLeaderboard::getId, exist.getId())
                    .set(GameLeaderboard::getScore, score)
                    .set(GameLeaderboard::getUpdatedAt, LocalDateTime.now());
            leaderboardMapper.update(null, updateWrapper);
        }
    }

    private GameLeaderboardVO toVO(GameLeaderboard lb) {
        if (lb == null) {
            return null;
        }
        GameLeaderboardVO vo = new GameLeaderboardVO();
        vo.setId(lb.getId());
        vo.setGameId(lb.getGameId());
        vo.setUserId(lb.getUserId());
        vo.setScore(lb.getScore());
        vo.setRank(lb.getRank());
        vo.setSeasonId(lb.getSeasonId());
        vo.setUpdatedAt(lb.getUpdatedAt());
        return vo;
    }
}
