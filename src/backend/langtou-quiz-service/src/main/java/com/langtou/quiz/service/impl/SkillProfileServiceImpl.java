package com.langtou.quiz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.quiz.dto.DifficultyContext;
import com.langtou.quiz.dto.SkillAnalysisResult;
import com.langtou.quiz.dto.SkillLevelResult;
import com.langtou.quiz.entity.UserSkillProfile;
import com.langtou.quiz.enums.SkillSubjectEnum;
import com.langtou.quiz.enums.TrendEnum;
import com.langtou.quiz.mapper.UserSkillProfileMapper;
import com.langtou.quiz.service.SkillProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillProfileServiceImpl implements SkillProfileService {

    private final UserSkillProfileMapper userSkillProfileMapper;

    @Override
    public UserSkillProfile getProfile(Long userId, String subject) {
        SkillSubjectEnum subjectEnum = SkillSubjectEnum.fromValue(subject);
        String subjectValue = subjectEnum != null ? subjectEnum.getValue() : subject;
        return userSkillProfileMapper.findByUserAndSubject(userId, subjectValue).orElseGet(
                () -> createDefaultProfile(userId, subjectValue));
    }

    @Override
    public List<UserSkillProfile> getProfiles(Long userId) {
        LambdaQueryWrapper<UserSkillProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSkillProfile::getUserId, userId);
        return userSkillProfileMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateAfterAnswer(Long userId, String subject, boolean isCorrect, int responseMs) {
        SkillSubjectEnum subjectEnum = SkillSubjectEnum.fromValue(subject);
        String subjectValue = subjectEnum != null ? subjectEnum.getValue() : subject;
        UserSkillProfile profile = userSkillProfileMapper.findByUserAndSubject(userId, subjectValue)
                .orElseGet(() -> createDefaultProfile(userId, subjectValue));

        int totalAnswered = safe(profile.getTotalQuestionsAnswered()) + 1;
        int totalCorrect = safe(profile.getTotalCorrect()) + (isCorrect ? 1 : 0);
        int totalWrong = safe(profile.getTotalWrong()) + (isCorrect ? 0 : 1);

        profile.setTotalQuestionsAnswered(totalAnswered);
        profile.setTotalCorrect(totalCorrect);
        profile.setTotalWrong(totalWrong);

        BigDecimal accuracy = totalAnswered == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalCorrect).divide(BigDecimal.valueOf(totalAnswered), 4, RoundingMode.HALF_UP);
        profile.setAccuracyRate(accuracy.setScale(2, RoundingMode.HALF_UP));

        BigDecimal oldLevel = profile.getSkillLevel() == null ? BigDecimal.valueOf(5) : profile.getSkillLevel();
        BigDecimal delta = BigDecimal.valueOf(isCorrect ? 0.05 : -0.08);
        BigDecimal newLevel = oldLevel.add(delta).setScale(2, RoundingMode.HALF_UP);
        if (newLevel.compareTo(BigDecimal.ONE) < 0) {
            newLevel = BigDecimal.ONE;
        }
        if (newLevel.compareTo(BigDecimal.TEN) > 0) {
            newLevel = BigDecimal.TEN;
        }
        profile.setSkillLevel(newLevel);

        BigDecimal diff = newLevel.subtract(oldLevel);
        if (diff.compareTo(BigDecimal.valueOf(0.01)) > 0) {
            profile.setTrend(TrendEnum.UP);
        } else if (diff.compareTo(BigDecimal.valueOf(-0.01)) < 0) {
            profile.setTrend(TrendEnum.DOWN);
        } else {
            profile.setTrend(TrendEnum.STABLE);
        }

        int oldAvg = profile.getAvgResponseMs() == null ? 30000 : profile.getAvgResponseMs();
        profile.setAvgResponseMs((int) Math.round((oldAvg * 0.7 + responseMs * 0.3)));

        profile.setConfidence(calculateConfidence(totalAnswered));
        profile.setLastAnsweredAt(LocalDateTime.now());

        if (profile.getId() == null) {
            userSkillProfileMapper.insert(profile);
        } else {
            userSkillProfileMapper.updateById(profile);
        }

        log.info("更新能力画像: userId={}, subject={}, level={}, accuracy={}",
                userId, subjectValue, newLevel, profile.getAccuracyRate());
    }

    @Override
    public SkillAnalysisResult analyzeProfile(Long userId, String subject) {
        UserSkillProfile profile = getProfile(userId, subject);
        SkillAnalysisResult result = new SkillAnalysisResult();
        result.setUserId(userId);
        result.setSubject(subject);
        result.setOverallSkillLevel(profile.getSkillLevel());
        result.setAccuracyRate(profile.getAccuracyRate());
        result.setConfidence(profile.getConfidence());
        result.setTrend(profile.getTrend() != null ? profile.getTrend().getValue() : "stable");
        result.setWeakPoints(profile.getWeakness() == null ? new ArrayList<>() : profile.getWeakness());
        result.setStrongPoints(deduceStrongPoints(profile));
        Map<String, Object> style = profile.getLearningStyle();
        result.setLearningStyle(style == null ? "unknown" : String.valueOf(style.getOrDefault("primary", "unknown")));
        result.setTotalAnswered(profile.getTotalQuestionsAnswered());
        result.setTotalCorrect(profile.getTotalCorrect());
        result.setTotalWrong(profile.getTotalWrong());
        result.setSuggestion(buildSuggestion(profile));
        return result;
    }

    @Override
    public SkillLevelResult predictNextDifficulty(Long userId, String subject, DifficultyContext context) {
        UserSkillProfile profile = getProfile(userId, subject);
        BigDecimal baseDifficulty = BigDecimal.TEN.subtract(profile.getSkillLevel());
        BigDecimal adjustment = BigDecimal.ZERO;
        StringBuilder reasonBuilder = new StringBuilder();

        adjustment = adjustment.add(calcRecentPerformanceAdj(profile, context, reasonBuilder));
        adjustment = adjustment.add(calcFatigueAdj(context, reasonBuilder));
        adjustment = adjustment.add(calcTimeOfDayAdj(context, reasonBuilder));
        adjustment = adjustment.add(calcGameStateAdj(context, reasonBuilder));

        BigDecimal recommended = baseDifficulty.add(adjustment).setScale(2, RoundingMode.HALF_UP);
        if (recommended.compareTo(BigDecimal.ONE) < 0) {
            recommended = BigDecimal.ONE;
        }
        if (recommended.compareTo(BigDecimal.TEN) > 0) {
            recommended = BigDecimal.TEN;
        }

        SkillLevelResult result = new SkillLevelResult();
        result.setSubject(subject);
        result.setRecommendedDifficulty(recommended);
        result.setLevel1To10(recommended.intValue());
        result.setStrategy(buildStrategy(recommended, context));
        result.setReason(reasonBuilder.length() == 0 ? "基于画像默认值" : reasonBuilder.toString());
        result.setConfidence(profile.getConfidence());
        return result;
    }

    private BigDecimal calcRecentPerformanceAdj(UserSkillProfile profile, DifficultyContext ctx, StringBuilder reason) {
        if (ctx == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal adj = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(ctx.getRecentCorrect())) {
            adj = adj.add(BigDecimal.valueOf(-0.5));
            reason.append("近期表现良好，降低难度 ");
        } else if (Boolean.FALSE.equals(ctx.getRecentCorrect())) {
            adj = adj.add(BigDecimal.valueOf(0.5));
            reason.append("近期表现欠佳，提升难度 ");
        }
        Integer streak = ctx.getRecentStreak();
        if (streak != null && streak >= 3) {
            adj = adj.add(BigDecimal.valueOf(streak >= 5 ? -0.8 : -0.4));
            reason.append("连对").append(streak).append("题，适当降难 ");
        } else if (streak != null && streak <= -2) {
            adj = adj.add(BigDecimal.valueOf(0.6));
            reason.append("连错 ").append(Math.abs(streak)).append("题，增加挑战 ");
        }
        return adj;
    }

    private BigDecimal calcFatigueAdj(DifficultyContext ctx, StringBuilder reason) {
        if (ctx == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal adj = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(ctx.getIsTired())) {
            adj = adj.add(BigDecimal.valueOf(-0.8));
            reason.append("已疲劳，降低难度 ");
        }
        Integer consecutive = ctx.getConsecutiveQuestions();
        if (consecutive != null && consecutive > 0) {
            if (consecutive >= 15) {
                adj = adj.add(BigDecimal.valueOf(-0.6));
                reason.append("连续答题过多，降难 ");
            } else if (consecutive > 8) {
                adj = adj.add(BigDecimal.valueOf(-0.3));
                reason.append("中等连续答题，微调降难 ");
            }
        }
        return adj;
    }

    private BigDecimal calcTimeOfDayAdj(DifficultyContext ctx, StringBuilder reason) {
        if (ctx == null || ctx.getCurrentTime() == null) {
            return BigDecimal.ZERO;
        }
        LocalTime t = ctx.getCurrentTime();
        BigDecimal adj = BigDecimal.ZERO;
        int hour = t.getHour();
        if (hour >= 0 && hour < 6) {
            adj = adj.add(BigDecimal.valueOf(-0.7));
            reason.append("深夜时段，降难 ");
        } else if (hour >= 12 && hour < 14) {
            adj = adj.add(BigDecimal.valueOf(-0.3));
            reason.append("午休时段，微调降难 ");
        } else if (hour >= 20 && hour < 23) {
            adj = adj.add(BigDecimal.valueOf(0.3));
            reason.append("黄金时段，适度提难 ");
        }
        return adj;
    }

    private BigDecimal calcGameStateAdj(DifficultyContext ctx, StringBuilder reason) {
        if (ctx == null || !Boolean.TRUE.equals(ctx.getInGame())) {
            return BigDecimal.ZERO;
        }
        BigDecimal adj = BigDecimal.ZERO;
        Integer lives = ctx.getLivesRemaining();
        if (lives != null) {
            if (lives <= 1) {
                adj = adj.add(BigDecimal.valueOf(-0.6));
                reason.append("生命危险，降难保命 ");
            } else if (lives >= 3) {
                adj = adj.add(BigDecimal.valueOf(0.3));
                reason.append("生命充足，提难 ");
            }
        }
        Integer score = ctx.getCurrentScore();
        if (score != null && score >= 80) {
            adj = adj.add(BigDecimal.valueOf(0.3));
            reason.append("高分段，提难 ");
        }
        if (Boolean.TRUE.equals(ctx.getIsRevived())) {
            adj = adj.add(BigDecimal.valueOf(-0.4));
            reason.append("复活后降难 ");
        }
        return adj;
    }

    private String buildStrategy(BigDecimal difficulty, DifficultyContext ctx) {
        double d = difficulty.doubleValue();
        if (d <= 3) {
            return "引导入门：侧重基础概念与简单回忆";
        } else if (d <= 6) {
            return "巩固练习：侧重知识点辨析与基本应用";
        } else if (d <= 8) {
            return "进阶挑战：侧重综合应用与易错辨析";
        } else {
            return "高手挑战：侧重深度推理与迁移应用";
        }
    }

    private String buildSuggestion(UserSkillProfile profile) {
        BigDecimal accuracy = profile.getAccuracyRate() == null ? BigDecimal.ZERO : profile.getAccuracyRate();
        BigDecimal level = profile.getSkillLevel() == null ? BigDecimal.valueOf(5) : profile.getSkillLevel();
        if (accuracy.compareTo(BigDecimal.valueOf(0.9)) >= 0) {
            return "掌握扎实，建议增加高阶题挑战，强化知识迁移能力。";
        }
        if (accuracy.compareTo(BigDecimal.valueOf(0.7)) >= 0) {
            return "基础良好，建议在巩固基础的同时引入变式与综合题。";
        }
        if (accuracy.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
            return "基础一般，建议加强核心知识点的循环练习，注意错题复盘。";
        }
        return "基础较弱，建议从基础概念重新梳理，配合提示系统逐步提升。";
    }

    private List<String> deduceStrongPoints(UserSkillProfile profile) {
        List<String> strong = new ArrayList<>();
        BigDecimal accuracy = profile.getAccuracyRate() == null ? BigDecimal.ZERO : profile.getAccuracyRate();
        if (accuracy.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            strong.add("知识点掌握");
        }
        BigDecimal level = profile.getSkillLevel() == null ? BigDecimal.valueOf(5) : profile.getSkillLevel();
        if (level.compareTo(BigDecimal.valueOf(7)) >= 0) {
            strong.add("综合应用能力");
        }
        Integer avgMs = profile.getAvgResponseMs();
        if (avgMs != null && avgMs < 20000) {
            strong.add("答题速度");
        }
        return strong;
    }

    private BigDecimal calculateConfidence(int totalAnswered) {
        if (totalAnswered <= 0) {
            return BigDecimal.valueOf(0.30);
        }
        if (totalAnswered < 5) {
            return BigDecimal.valueOf(0.40);
        }
        if (totalAnswered < 20) {
            return BigDecimal.valueOf(0.60);
        }
        if (totalAnswered < 50) {
            return BigDecimal.valueOf(0.80);
        }
        return BigDecimal.valueOf(0.90);
    }

    private UserSkillProfile createDefaultProfile(Long userId, String subject) {
        UserSkillProfile p = new UserSkillProfile();
        p.setUserId(userId);
        p.setSubject(SkillSubjectEnum.fromValue(subject));
        p.setSkillLevel(BigDecimal.valueOf(5));
        p.setConfidence(BigDecimal.valueOf(0.30));
        p.setTrend(TrendEnum.STABLE);
        p.setAvgResponseMs(30000);
        p.setAccuracyRate(BigDecimal.valueOf(0.50));
        p.setTotalQuestionsAnswered(0);
        p.setTotalCorrect(0);
        p.setTotalWrong(0);
        userSkillProfileMapper.insert(p);
        return p;
    }

    private int safe(Integer v) {
        return v == null ? 0 : v;
    }
}
