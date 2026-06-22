package com.langtou.quiz.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.langtou.quiz.dto.EmotionContext;
import com.langtou.quiz.dto.EmotionDetectionResult;
import com.langtou.quiz.dto.EmotionState;
import com.langtou.quiz.dto.EmotionalResponse;
import com.langtou.quiz.entity.UserEmotionState;
import com.langtou.quiz.enums.EmotionEnum;
import com.langtou.quiz.mapper.UserEmotionStateMapper;
import com.langtou.quiz.service.EmotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionServiceImpl implements EmotionService {

    private final UserEmotionStateMapper emotionStateMapper;

    private static final Map<EmotionEnum, List<String>> RESPONSE_LIBRARY = new ConcurrentHashMap<>();

    static {
        RESPONSE_LIBRARY.put(EmotionEnum.FRUSTRATION, Arrays.asList(
                "别急，我们把这道题拆开来看看。",
                "这题确实有点绕，要不要试试提示？",
                "每一次挫败都是进步的铺垫，我陪你再来一次。",
                "深呼吸，换个思路试试。",
                "卡住没关系，关键是你没有放弃！",
                "你已经走得很远了，我来帮你理一理思路。",
                "让我们把大问题切成小问题。",
                "你的坚持本身就很棒。",
                "试试排除法，往往有意想不到的效果。",
                "我感受到你的专注了，我们慢一点也没关系。"
        ));
        RESPONSE_LIBRARY.put(EmotionEnum.FLOW, Arrays.asList(
                "✨ 你已经进入心流状态，继续保持！",
                "✨ 这个节奏太舒服了，我都替你开心。",
                "✨ 思维非常流畅，趁热打铁！",
                "✨ 你在发光，继续相信自己！",
                "✨ 现在的你是最强版本！",
                "✨ 状态拉满，这就是你应得的！",
                "✨ 大脑飞速运转，棒极了！",
                "✨ 你已经完全沉浸，无敌是多么寂寞。",
                "✨ 这种顺畅通关的感觉真好。",
                "✨ 保持专注，胜利在望！"
        ));
        RESPONSE_LIBRARY.put(EmotionEnum.BOREDOM, Arrays.asList(
                "😴 这道题太简单啦，来道挑战题？",
                "😴 我给你加点难度，准备好了吗？",
                "😴 换个题型玩玩？",
                "😴 热身结束，咱们进入正题！",
                "😴 这些小菜一碟，要不要挑战高难度？",
                "😴 我看你有点走神，来杯咖啡提提神？",
                "😴 基础稳了，是时候突破舒适区了。",
                "😴 一直做同类型题目可没意思。",
                "😴 升级打怪吧，我看好你！",
                "😴 让我们给你来点刺激的。"
        ));
        RESPONSE_LIBRARY.put(EmotionEnum.EXCITEMENT, Arrays.asList(
                "🤩 连对！你就是今天的学霸！",
                "🤩 这波操作太秀了！",
                "🤩 太强啦，继续冲！",
                "🤩 连胜继续，势不可挡！",
                "🤩 我感受到你的能量了！",
                "🤩 这就是强者的姿态！",
                "🤩 节奏完美，乘胜追击！",
                "🤩 你的进步肉眼可见！",
                "🤩 再对一题就解锁新成就！",
                "🤩 太精彩啦，继续发光！"
        ));
        RESPONSE_LIBRARY.put(EmotionEnum.CALM, Arrays.asList(
                "😊 保持现在的节奏，你可以的。",
                "😊 一步一步来，稳定发挥。",
                "😊 稳扎稳打，胜利属于你。",
                "😊 平静也是一种力量。",
                "😊 细心检查，答案就在眼前。",
                "😊 不急不躁，你做得很好。",
                "😊 专注当下，一切都在掌控中。",
                "😊 你今天状态不错，继续保持。",
                "😊 每一次答题都是成长。",
                "😊 我会一直陪着你的。"
        ));
    }

    @Override
    public EmotionDetectionResult detectEmotion(Long userId, EmotionContext context) {
        EmotionDetectionResult result = new EmotionDetectionResult();
        EmotionEnum detected = EmotionEnum.CALM;
        BigDecimal confidence = BigDecimal.valueOf(0.6);
        StringBuilder reason = new StringBuilder();

        Long avgMs = context.getAvgResponseMs();
        Long lastMs = context.getLastResponseMs();
        Double accuracy = context.getAccuracyRate() == null ? 0.0 : context.getAccuracyRate();
        Integer duration = context.getSessionDurationSeconds() == null ? 0 : context.getSessionDurationSeconds();
        Integer recentCorrect = context.getRecentCorrectCount() == null ? 0 : context.getRecentCorrectCount();
        Integer recentWrong = context.getRecentWrongCount() == null ? 0 : context.getRecentWrongCount();

        if (lastMs != null && avgMs != null && avgMs > 0) {
            double ratio = (double) lastMs / avgMs;
            if (ratio > 1.5 && accuracy < 0.30) {
                detected = EmotionEnum.FRUSTRATION;
                confidence = BigDecimal.valueOf(0.9);
                reason.append("响应耗时为平均 ").append(String.format("%.2f", ratio)).append(" 倍 且 正确率仅 ")
                        .append(String.format("%.0f%%", accuracy * 100));
            } else if (ratio < 0.8 && accuracy > 0.90) {
                detected = EmotionEnum.FLOW;
                confidence = BigDecimal.valueOf(0.88);
                reason.append("响应迅捷为平均 ").append(String.format("%.2f", ratio)).append(" 倍 且 正确率 ")
                        .append(String.format("%.0f%%", accuracy * 100));
            }
        }

        if (duration >= 900 && accuracy > 0.95 && detected == EmotionEnum.CALM) {
            detected = EmotionEnum.BOREDOM;
            confidence = BigDecimal.valueOf(0.82);
            reason.append("连续答题 ").append(duration / 60).append(" 分钟 且 正确率高达 ")
                    .append(String.format("%.0f%%", accuracy * 100)).append("，可能进入倦怠期");
        }

        if (recentCorrect >= 5 && detected == EmotionEnum.CALM) {
            detected = EmotionEnum.EXCITEMENT;
            confidence = BigDecimal.valueOf(0.85);
            reason.append("连对 ").append(recentCorrect).append(" 题，进入兴奋状态");
        }

        if (detected == EmotionEnum.CALM && reason.length() == 0) {
            reason.append("未检测到明显情绪触发，保持平静");
        }

        result.setEmotion(detected);
        result.setConfidence(confidence);
        result.setReason(reason.toString());
        result.setResponseMs(lastMs);
        result.setAccuracyRate(accuracy);
        result.setStreak(recentCorrect - recentWrong);

        recordEmotionInternal(userId, detected, reason.toString(), recentCorrect, recentWrong, context);
        return result;
    }

    @Override
    public EmotionalResponse generateResponse(Long userId, EmotionEnum emotion) {
        EmotionalResponse response = new EmotionalResponse();
        EmotionEnum target = emotion == null ? EmotionEnum.CALM : emotion;
        List<String> candidates = RESPONSE_LIBRARY.getOrDefault(target, RESPONSE_LIBRARY.get(EmotionEnum.CALM));

        UserEmotionState latest = emotionStateMapper.findLatestByUserId(userId).orElse(null);
        int index = 0;
        if (latest != null && latest.getConsecutiveCorrect() != null) {
            index = Math.abs(latest.getConsecutiveCorrect() * 3 + (int) (System.currentTimeMillis() % 17)) % candidates.size();
        } else {
            index = (int) (System.currentTimeMillis() % candidates.size());
        }

        String text = candidates.get(index);
        response.setEmotion(target);
        response.setText(text);
        response.setTone(resolveTone(target));
        response.setActionSuggestion(resolveAction(target));
        response.setCompanionReply(text);
        return response;
    }

    @Override
    @Transactional
    public void recordEmotion(Long userId, EmotionEnum emotion, String trigger) {
        recordEmotionInternal(userId, emotion, trigger, 0, 0, null);
    }

    @Override
    public EmotionState getCurrentState(Long userId) {
        UserEmotionState latest = emotionStateMapper.findLatestByUserId(userId).orElse(null);
        EmotionState state = new EmotionState();
        if (latest == null) {
            state.setCurrentEmotion(EmotionEnum.CALM);
            state.setConfidence(BigDecimal.valueOf(0.5));
            return state;
        }
        state.setCurrentEmotion(latest.getCurrentEmotion());
        state.setPreviousEmotion(latest.getPreviousEmotion());
        state.setConfidence(BigDecimal.valueOf(0.8));
        state.setConsecutiveCorrect(latest.getConsecutiveCorrect());
        state.setConsecutiveWrong(latest.getConsecutiveWrong());
        state.setSessionQuestions(latest.getSessionQuestions());
        state.setAvgResponseMs(latest.getAvgResponseMs());
        state.setSessionDurationSeconds(latest.getSessionDurationSeconds());
        state.setLastTrigger(latest.getTrigger());
        state.setDetectedAt(latest.getDetectedAt() == null ? null : latest.getDetectedAt().toEpochMilli());

        Double accuracy = null;
        Integer total = latest.getSessionQuestions() == null ? 0 : latest.getSessionQuestions();
        Integer correct = latest.getSessionCorrect() == null ? 0 : latest.getSessionCorrect();
        if (total > 0) {
            accuracy = BigDecimal.valueOf(correct)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        state.setAccuracyRate(accuracy);
        return state;
    }

    private void recordEmotionInternal(Long userId, EmotionEnum emotion, String trigger,
                                       int recentCorrect, int recentWrong, EmotionContext context) {
        UserEmotionState existing = emotionStateMapper.findLatestByUserId(userId).orElse(null);
        UserEmotionState state = new UserEmotionState();
        state.setUserId(userId);
        state.setCurrentEmotion(emotion);
        state.setPreviousEmotion(existing == null ? null : existing.getCurrentEmotion());
        state.setConsecutiveCorrect(recentCorrect);
        state.setConsecutiveWrong(recentWrong);
        state.setTrigger(trigger);
        state.setDetectedAt(LocalDateTime.now());

        if (context != null) {
            state.setAvgResponseMs(context.getAvgResponseMs());
            state.setLastResponseMs(context.getLastResponseMs());
            state.setSessionDurationSeconds(context.getSessionDurationSeconds());
            state.setSessionQuestions(context.getSessionQuestions());
            state.setSessionCorrect(recentCorrect);
        } else if (existing != null) {
            state.setAvgResponseMs(existing.getAvgResponseMs());
            state.setLastResponseMs(existing.getLastResponseMs());
            state.setSessionDurationSeconds(existing.getSessionDurationSeconds());
            state.setSessionQuestions(existing.getSessionQuestions());
            state.setSessionCorrect(existing.getSessionCorrect());
        }

        emotionStateMapper.insert(state);
        log.info("记录情绪: userId={}, emotion={}, trigger={}", userId, emotion, trigger);
    }

    private String resolveTone(EmotionEnum emotion) {
        if (emotion == null) {
            return "warm";
        }
        switch (emotion) {
            case FRUSTRATION:
                return "gentle";
            case FLOW:
                return "energetic";
            case BOREDOM:
                return "playful";
            case EXCITEMENT:
                return "enthusiastic";
            case CALM:
            default:
                return "warm";
        }
    }

    private String resolveAction(EmotionEnum emotion) {
        if (emotion == null) {
            return "continue";
        }
        switch (emotion) {
            case FRUSTRATION:
                return "建议使用提示或放慢节奏";
            case FLOW:
                return "保持当前节奏，不要中断";
            case BOREDOM:
                return "建议切换到更高难度";
            case EXCITEMENT:
                return "乘胜追击，挑战连胜记录";
            case CALM:
            default:
                return "继续稳定答题";
        }
    }
}
