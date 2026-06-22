package com.langtou.quiz.service;

import com.langtou.quiz.dto.LeaderboardEntry;

import java.util.List;

public interface LeaderboardService {

    void updateBestScore(Long userId, Long quizSetId, int score);

    List<LeaderboardEntry> getQuizLeaderboard(Long quizSetId, int limit);

    List<LeaderboardEntry> getGlobalLeaderboard(int limit);

    List<LeaderboardEntry> getFriendLeaderboard(Long userId, int limit);
}
