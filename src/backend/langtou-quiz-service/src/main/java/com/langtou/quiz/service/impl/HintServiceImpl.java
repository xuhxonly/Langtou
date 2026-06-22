package com.langtou.quiz.service.impl;

import com.langtou.quiz.dto.HintResponse;
import com.langtou.quiz.dto.HintResult;
import com.langtou.quiz.entity.QuizHintLog;
import com.langtou.quiz.entity.QuizQuestion;
import com.langtou.quiz.mapper.QuizHintLogMapper;
import com.langtou.quiz.mapper.QuizQuestionMapper;
import com.langtou.quiz.service.HintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintServiceImpl implements HintService {

    private final QuizQuestionMapper quizQuestionMapper;
    private final QuizHintLogMapper quizHintLogMapper;

    private static final int MAX_HINT_LEVEL = 4;

    @Override
    public HintResponse requestHint(Long userId, Long quizSetId, Long questionId, int currentHintLevel) {
        if (currentHintLevel < 0) {
            currentHintLevel = 0;
        }
        if (currentHintLevel >= MAX_HINT_LEVEL) {
            HintResponse resp = new HintResponse();
            resp.setQuestionId(questionId);
            resp.setCurrentHintLevel(MAX_HINT_LEVEL);
            resp.setNextHintLevel(MAX_HINT_LEVEL);
            resp.setHasMoreHints(false);
            resp.setHintContent("已达到最高提示层级，请直接查看解析或选择放弃。");
            resp.setStrategy("no_more_hints");
            return resp;
        }

        int nextLevel = currentHintLevel + 1;
        QuizQuestion question = quizQuestionMapper.selectById(questionId);
        if (question == null) {
            HintResponse resp = new HintResponse();
            resp.setQuestionId(questionId);
            resp.setCurrentHintLevel(currentHintLevel);
            resp.setNextHintLevel(currentHintLevel);
            resp.setHasMoreHints(false);
            resp.setHintContent("题目不存在，无法提供提示。");
            resp.setStrategy("error");
            return resp;
        }

        HintResult hint = getHintByLevel(nextLevel, question);
        recordHintRequest(userId, quizSetId, questionId, nextLevel, hint.getContent());

        HintResponse resp = new HintResponse();
        resp.setQuestionId(questionId);
        resp.setCurrentHintLevel(currentHintLevel);
        resp.setNextHintLevel(nextLevel);
        resp.setHintContent(hint.getContent());
        resp.setHasMoreHints(nextLevel < MAX_HINT_LEVEL);
        resp.setStrategy("hint_level_" + nextLevel);
        return resp;
    }

    @Override
    public HintResult getHintByLevel(int level, QuizQuestion question) {
        HintResult result = new HintResult();
        result.setLevel(level);

        if (level == 1) {
            result.setTitle("方向性提示");
            result.setHintType("direction");
            result.setContent(buildLevel1Hint(question));
        } else if (level == 2) {
            result.setTitle("线索提示");
            result.setHintType("clue");
            result.setContent(buildLevel2Hint(question));
        } else if (level == 3) {
            result.setTitle("关键知识提示");
            result.setHintType("knowledge");
            result.setContent(buildLevel3Hint(question));
        } else if (level == 4) {
            result.setTitle("思路引导");
            result.setHintType("approach");
            result.setContent(buildLevel4Hint(question));
        } else {
            result.setTitle("未知提示");
            result.setHintType("unknown");
            result.setContent("该层级提示不存在。");
        }
        return result;
    }

    @Override
    public void recordHintRequest(Long userId, Long quizSetId, Long questionId, int level, String hintContent) {
        QuizHintLog logEntity = new QuizHintLog();
        logEntity.setUserId(userId);
        logEntity.setQuizSetId(quizSetId);
        logEntity.setQuestionId(questionId);
        logEntity.setHintLevel(level);
        logEntity.setHintContent(hintContent == null ? null : hintContent.length() > 500 ? hintContent.substring(0, 500) : hintContent);
        quizHintLogMapper.insert(logEntity);
        log.debug("记录提示请求: userId={}, questionId={}, level={}", userId, questionId, level);
    }

    private String buildLevel1Hint(QuizQuestion q) {
        StringBuilder sb = new StringBuilder();
        sb.append("请先理解题干的核心考察方向：");
        String stem = q.getStem() == null ? "" : q.getStem();
        if (stem.length() > 60) {
            sb.append(stem, 0, 60).append("...");
        } else {
            sb.append(stem);
        }
        sb.append("。抓住关键词，思考它最可能归属的知识模块。");
        return sb.toString();
    }

    private String buildLevel2Hint(QuizQuestion q) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下选项大概率可以排除：");
        String correct = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().trim();
        char[] excluded = new char[2];
        int idx = 0;
        for (char c : new char[]{'A', 'B', 'C', 'D'}) {
            if (!String.valueOf(c).equalsIgnoreCase(correct) && idx < 2) {
                excluded[idx++] = c;
            }
        }
        if (idx == 0) {
            sb.append("暂无可靠排除项，请继续思考。");
        } else {
            sb.append(excluded[0]);
            if (idx > 1) {
                sb.append("、").append(excluded[1]);
            }
            sb.append("。优先在余下选项中做判断。");
        }
        return sb.toString();
    }

    private String buildLevel3Hint(QuizQuestion q) {
        StringBuilder sb = new StringBuilder();
        sb.append("关键知识点：");
        if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
            String expl = q.getExplanation();
            int limit = Math.min(120, expl.length());
            sb.append(expl, 0, limit);
            if (expl.length() > limit) {
                sb.append("...");
            }
        } else {
            sb.append("请回忆与题干相关的核心概念、公式或定理，并结合题目条件进行推导。");
        }
        return sb.toString();
    }

    private String buildLevel4Hint(QuizQuestion q) {
        StringBuilder sb = new StringBuilder();
        sb.append("思路引导：");
        sb.append("第一步，拆解题干中的已知条件；第二步，匹配对应的知识点或公式；");
        sb.append("第三步，结合选项逐一验证。");
        if (q.getCorrectAnswer() != null && !q.getCorrectAnswer().isEmpty()) {
            sb.append(" 提示：正确选项的字母为 ").append(q.getCorrectAnswer()).append("，请自行验证推导过程。");
        }
        return sb.toString();
    }
}
