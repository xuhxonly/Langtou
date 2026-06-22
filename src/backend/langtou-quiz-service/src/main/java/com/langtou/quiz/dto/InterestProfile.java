package com.langtou.quiz.dto;

import lombok.Data;

import java.util.List;

@Data
public class InterestProfile {

    private Long userId;

    private List<String> topics;

    private List<String> domains;

    private List<String> themes;

    private String primaryInterest;

    private String secondaryInterest;

    private Integer diversityScore;
}
