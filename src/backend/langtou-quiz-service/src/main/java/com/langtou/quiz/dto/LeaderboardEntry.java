package com.langtou.quiz.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LeaderboardEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String avatarUrl;

    private Integer score;

    private Long rank;

    private Long quizSetId;

    private Long updatedAt;

    public LeaderboardEntry() {
    }

    public LeaderboardEntry(Long userId, Integer score) {
        this.userId = userId;
        this.score = score;
    }
}
