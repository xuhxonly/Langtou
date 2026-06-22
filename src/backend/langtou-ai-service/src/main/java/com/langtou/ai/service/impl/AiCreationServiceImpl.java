package com.langtou.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.langtou.ai.dto.AiCoverRecommendRequest;
import com.langtou.ai.dto.AiCoverRecommendResponse;
import com.langtou.ai.dto.AiCoverSuggestion;
import com.langtou.ai.dto.AiDraftRequest;
import com.langtou.ai.dto.AiDraftResponse;
import com.langtou.ai.dto.AiTagRecommendRequest;
import com.langtou.ai.dto.AiTagRecommendResponse;
import com.langtou.ai.dto.AiTagSuggestion;
import com.langtou.ai.dto.AiTitleRequest;
import com.langtou.ai.dto.AiTitleResponse;
import com.langtou.ai.dto.AiTitleSuggestion;
import com.langtou.ai.service.AiCreationService;
import com.langtou.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCreationServiceImpl implements AiCreationService {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${langtou.ai.creation.enabled:true}")
    private boolean aiCreationEnabled;

    @Value("${langtou.ai.creation.provider:mock}")
    private String aiProvider;

    @Value("${langtou.ai.creation.api-key:}")
    private String apiKey;

    @Value("${langtou.ai.creation.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String apiUrl;

    @Value("${langtou.ai.creation.model:qwen-turbo}")
    private String model;

    @Value("${langtou.ai.creation.rate-limit:10}")
    private int rateLimit;

    @Value("${langtou.ai.creation.cache-ttl:3600}")
    private long cacheTtl;

    private static final String RATE_LIMIT_PREFIX = "ai:rate_limit:";
    private static final String CACHE_PREFIX = "ai:cache:";

    @Override
    public AiTitleResponse generateTitles(AiTitleRequest request) {
        checkEnabled();
        String cacheKey = buildCacheKey("title", request.getImageUrls(), request.getContentType());

        AiTitleResponse cached = getFromCache(cacheKey, AiTitleResponse.class);
        if (cached != null) {
            log.info("AI title cache hit: cacheKey={}", cacheKey);
            return cached;
        }

        if ("mock".equalsIgnoreCase(aiProvider)) {
            AiTitleResponse response = generateMockTitles();
            saveToCache(cacheKey, response);
            return response;
        }

        String prompt = buildTitlePrompt(request.getImageUrls());
        String aiResponse = callAiApi(prompt);
        AiTitleResponse response = parseTitleResponse(aiResponse);
        saveToCache(cacheKey, response);
        return response;
    }

    @Override
    public AiTagRecommendResponse recommendTags(AiTagRecommendRequest request) {
        checkEnabled();
        String cacheKey = buildCacheKey("tags", request.getImageUrls(), request.getTitle(), request.getContent());

        AiTagRecommendResponse cached = getFromCache(cacheKey, AiTagRecommendResponse.class);
        if (cached != null) {
            log.info("AI tag cache hit: cacheKey={}", cacheKey);
            return cached;
        }

        if ("mock".equalsIgnoreCase(aiProvider)) {
            AiTagRecommendResponse response = generateMockTags();
            saveToCache(cacheKey, response);
            return response;
        }

        String prompt = buildTagPrompt(request.getTitle(), request.getContent());
        String aiResponse = callAiApi(prompt);
        AiTagRecommendResponse response = parseTagResponse(aiResponse);
        saveToCache(cacheKey, response);
        return response;
    }

    @Override
    public AiDraftResponse generateDraft(AiDraftRequest request) {
        checkEnabled();
        String cacheKey = buildCacheKey("draft", Collections.singletonList(request.getTitle()), request.getStyle(),
                request.getKeywords() != null ? String.join(",", request.getKeywords()) : "");

        AiDraftResponse cached = getFromCache(cacheKey, AiDraftResponse.class);
        if (cached != null) {
            log.info("AI draft cache hit: cacheKey={}", cacheKey);
            return cached;
        }

        if ("mock".equalsIgnoreCase(aiProvider)) {
            AiDraftResponse response = generateMockDraft(request.getStyle());
            saveToCache(cacheKey, response);
            return response;
        }

        String prompt = buildDraftPrompt(request.getTitle(), request.getStyle(), request.getKeywords());
        String aiResponse = callAiApi(prompt);
        AiDraftResponse response = parseDraftResponse(aiResponse);
        saveToCache(cacheKey, response);
        return response;
    }

    @Override
    public AiCoverRecommendResponse recommendCover(AiCoverRecommendRequest request) {
        checkEnabled();
        String cacheKey = buildCacheKey("cover", request.getImageUrls());

        AiCoverRecommendResponse cached = getFromCache(cacheKey, AiCoverRecommendResponse.class);
        if (cached != null) {
            log.info("AI cover cache hit: cacheKey={}", cacheKey);
            return cached;
        }

        if ("mock".equalsIgnoreCase(aiProvider)) {
            AiCoverRecommendResponse response = generateMockCover(request.getImageUrls());
            saveToCache(cacheKey, response);
            return response;
        }

        String prompt = buildCoverPrompt(request.getImageUrls());
        String aiResponse = callAiApi(prompt);
        AiCoverRecommendResponse response = parseCoverResponse(aiResponse, request.getImageUrls());
        saveToCache(cacheKey, response);
        return response;
    }

    @Override
    public void checkRateLimit(Long userId) {
        String key = RATE_LIMIT_PREFIX + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        if (count >= rateLimit) {
            log.warn("AI rate limit exceeded: userId={}, count={}, limit={}", userId, count, rateLimit);
            throw new BusinessException(429, "AI调用次数已达上限（每小时" + rateLimit + "次），请稍后再试");
        }

        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    private void checkEnabled() {
        if (!aiCreationEnabled) {
            throw new BusinessException(503, "AI创作功能暂未开放");
        }
    }

    private String callAiApi(String prompt) {
        log.info("Call AI API: provider={}, model={}", aiProvider, model);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", Collections.singletonList(message));

            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("output").path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
                choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
            }

            log.error("AI API call failed: status={}", response.getStatusCode());
            throw new BusinessException(50004, "AI服务异常，请稍后重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI API call exception", e);
            throw new BusinessException(50004, "AI服务异常，请稍后重试");
        }
    }

    private String buildTitlePrompt(List<String> imageUrls) {
        return "你是一位专业的小红书风格内容创作者，擅长写出吸引眼球、引发互动的笔记标题。\n\n" +
                "## 任务\n" +
                "根据以下图片信息，生成5个笔记标题。\n\n" +
                "## 图片信息\n" +
                "用户上传了" + imageUrls.size() + "张图片。\n\n" +
                "## 标题要求\n" +
                "1. 字数控制在10-30个字符\n" +
                "2. 覆盖不同风格：种草、测评、教程、Vlog、情感\n" +
                "3. 包含高频搜索关键词\n" +
                "4. 使用适当的标点符号增强表现力\n\n" +
                "## 输出格式\n" +
                "请以JSON数组格式输出，每个标题包含：\n" +
                "- title: 标题内容\n" +
                "- reason: 推荐理由\n" +
                "- score: 推荐分数(0-100)\n\n" +
                "示例输出:\n" +
                "[{\"title\":\"...\", \"reason\":\"...\", \"score\":90}, ...]\n\n" +
                "请直接输出JSON数组，不要包含其他内容。";
    }

    private String buildTagPrompt(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位社交媒体运营专家，擅长为内容选择精准的话题标签。\n\n");
        sb.append("## 任务\n");
        sb.append("根据以下信息，推荐10个话题标签。\n\n");

        if (title != null && !title.isEmpty()) {
            sb.append("## 标题\n").append(title).append("\n\n");
        }
        if (content != null && !content.isEmpty()) {
            sb.append("## 内容摘要\n").append(content.length() > 200 ? content.substring(0, 200) : content).append("\n\n");
        }

        sb.append("## 标签要求\n");
        sb.append("1. 推荐标签需与内容高度相关\n");
        sb.append("2. 覆盖不同类型：核心标签(core)、场景标签(scene)、风格标签(style)、情绪标签(emotion)\n");
        sb.append("3. 优先推荐热门标签\n");
        sb.append("4. 标签名2-8个字符\n\n");
        sb.append("## 输出格式\n");
        sb.append("请以JSON数组格式输出，每个标签包含：\n");
        sb.append("- tagName: 标签名\n");
        sb.append("- heat: 热度值(0-100000)\n");
        sb.append("- category: 分类(core/scene/style/emotion)\n\n");
        sb.append("请直接输出JSON数组，不要包含其他内容。");

        return sb.toString();
    }

    private String buildDraftPrompt(String title, String style, List<String> keywords) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的小红书风格内容创作者，擅长写出真实、有感染力的笔记文案。\n\n");
        sb.append("## 任务\n");
        sb.append("根据以下信息生成一篇完整的笔记正文草稿。\n\n");
        sb.append("## 标题\n").append(title).append("\n\n");
        sb.append("## 文案风格\n").append(style).append("\n\n");

        if (keywords != null && !keywords.isEmpty()) {
            sb.append("## 关键词\n").append(String.join("、", keywords)).append("\n\n");
        }

        sb.append("## 文案要求\n");
        sb.append("1. 字数300-800字\n");
        sb.append("2. 风格: ").append(getStyleDescription(style)).append("\n");
        sb.append("3. 段落清晰，适当使用emoji\n");
        sb.append("4. 结尾包含互动引导（提问/号召行动）\n");
        sb.append("5. 语气自然真实，避免过度营销感\n\n");
        sb.append("## 输出格式\n");
        sb.append("直接输出文案内容，使用纯文本格式。");

        return sb.toString();
    }

    private String buildCoverPrompt(List<String> imageUrls) {
        return "你是一位专业的摄影和视觉设计专家，擅长评估图片质量。\n\n" +
                "## 任务\n" +
                "评估" + imageUrls.size() + "张图片作为社交媒体封面的适合程度。\n\n" +
                "## 评估维度\n" +
                "1. 构图质量(composition): 三分法、黄金比例、对称性 (0-100)\n" +
                "2. 色彩丰富度(color): 色彩饱和度、对比度、色彩搭配 (0-100)\n" +
                "3. 清晰度(clarity): 图像锐度、分辨率 (0-100)\n\n" +
                "## 输出格式\n" +
                "请以JSON数组格式输出，每张图片包含：\n" +
                "- imageUrl: 图片URL\n" +
                "- score: 综合评分(0-100)\n" +
                "- compositionScore: 构图评分\n" +
                "- colorScore: 色彩评分\n" +
                "- clarityScore: 清晰度评分\n\n" +
                "请直接输出JSON数组，不要包含其他内容。";
    }

    private String getStyleDescription(String style) {
        return switch (style) {
            case "种草" -> "热情推荐，强调个人体验和推荐，语气亲切有感染力";
            case "测评" -> "客观分析，有理有据，信息量大，语气专业理性";
            case "教程" -> "步骤清晰，实用性强，语气耐心细致";
            case "Vlog" -> "叙事性强，有故事线，代入感强，像和朋友聊天";
            default -> "自然流畅的笔记风格";
        };
    }

    private AiTitleResponse parseTitleResponse(String aiResponse) {
        try {
            String json = extractJsonArray(aiResponse);
            List<AiTitleSuggestion> titles = new ArrayList<>();
            JsonNode array = objectMapper.readTree(json);

            for (JsonNode node : array) {
                AiTitleSuggestion suggestion = new AiTitleSuggestion();
                suggestion.setTitle(node.path("title").asText());
                suggestion.setReason(node.path("reason").asText());
                suggestion.setScore(node.path("score").asDouble(80.0));
                titles.add(suggestion);
            }

            return new AiTitleResponse(titles);
        } catch (Exception e) {
            log.error("Failed to parse AI title response: {}", aiResponse, e);
            return generateMockTitles();
        }
    }

    private AiTagRecommendResponse parseTagResponse(String aiResponse) {
        try {
            String json = extractJsonArray(aiResponse);
            List<AiTagSuggestion> tags = new ArrayList<>();
            JsonNode array = objectMapper.readTree(json);

            for (JsonNode node : array) {
                AiTagSuggestion suggestion = new AiTagSuggestion();
                suggestion.setTagName(node.path("tagName").asText());
                suggestion.setHeat(node.path("heat").asLong(50000L));
                suggestion.setCategory(node.path("category").asText("core"));
                tags.add(suggestion);
            }

            return new AiTagRecommendResponse(tags);
        } catch (Exception e) {
            log.error("Failed to parse AI tag response: {}", aiResponse, e);
            return generateMockTags();
        }
    }

    private AiDraftResponse parseDraftResponse(String aiResponse) {
        String draft = aiResponse.trim();
        int wordCount = draft.length();
        return new AiDraftResponse(draft, wordCount);
    }

    private AiCoverRecommendResponse parseCoverResponse(String aiResponse, List<String> imageUrls) {
        try {
            String json = extractJsonArray(aiResponse);
            List<AiCoverSuggestion> suggestions = new ArrayList<>();
            JsonNode array = objectMapper.readTree(json);

            for (JsonNode node : array) {
                AiCoverSuggestion suggestion = new AiCoverSuggestion();
                suggestion.setImageUrl(node.path("imageUrl").asText());
                suggestion.setScore(node.path("score").asDouble(80.0));
                suggestion.setCompositionScore(node.path("compositionScore").asDouble(80.0));
                suggestion.setColorScore(node.path("colorScore").asDouble(80.0));
                suggestion.setClarityScore(node.path("clarityScore").asDouble(80.0));
                suggestion.setCropUrl(node.path("imageUrl").asText() + "?crop=3:4");
                suggestions.add(suggestion);
            }

            suggestions.sort(Comparator.comparingDouble(AiCoverSuggestion::getScore).reversed());
            return new AiCoverRecommendResponse(suggestions);
        } catch (Exception e) {
            log.error("Failed to parse AI cover response: {}", aiResponse, e);
            return generateMockCover(imageUrls);
        }
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private AiTitleResponse generateMockTitles() {
        List<AiTitleSuggestion> titles = Arrays.asList(
                new AiTitleSuggestion("今日份的好物分享，这些你一定不能错过！",
                        "使用了\"好物分享\"\"一定不能错过\"等高频种草词汇，容易引发好奇和互动", 92.0),
                new AiTitleSuggestion("周末探店全记录 | 这家店让我反复回购",
                        "信息量大，包含场景和体验，适合搜索流量", 88.0),
                new AiTitleSuggestion("手把手教你挑选性价比超高的好物",
                        "教程类标题，承诺实用价值，点击率高", 85.0),
                new AiTitleSuggestion("终于找到了！被朋友问了一百遍的宝藏推荐",
                        "情感共鸣型标题，\"终于\"\"宝藏\"等词汇引发好奇心", 90.0),
                new AiTitleSuggestion("生活好物合集 | 这5样东西让幸福感直线提升",
                        "列表型标题，信息密度高，收藏率高", 87.0)
        );
        return new AiTitleResponse(titles);
    }

    private AiTagRecommendResponse generateMockTags() {
        List<AiTagSuggestion> tags = Arrays.asList(
                new AiTagSuggestion("好物分享", 100000L, "core"),
                new AiTagSuggestion("探店打卡", 85000L, "scene"),
                new AiTagSuggestion("种草推荐", 92000L, "core"),
                new AiTagSuggestion("生活好物", 78000L, "core"),
                new AiTagSuggestion("周末去哪儿", 68000L, "scene"),
                new AiTagSuggestion("ins风", 55000L, "style"),
                new AiTagSuggestion("治愈系", 50000L, "emotion"),
                new AiTagSuggestion("小确幸", 45000L, "emotion"),
                new AiTagSuggestion("性价比", 60000L, "core"),
                new AiTagSuggestion("日常分享", 35000L, "scene")
        );
        return new AiTagRecommendResponse(tags);
    }

    private AiDraftResponse generateMockDraft(String style) {
        String draft;
        switch (style != null ? style : "种草") {
            case "测评" -> {
                draft = "今天给大家做一个详细的好物测评。\n\n" +
                        "最近入手了好几样超火的产品，用了一段时间后来给大家做个真实测评。\n\n" +
                        "第一款：颜值在线，质感很好，使用体验流畅，性价比4.5/5分。\n" +
                        "第二款：功能强大，但上手需要一定学习成本，适合有一定基础的朋友，综合评分4.0/5分。\n" +
                        "第三款：小巧便携，日常使用完全够用，价格也很友好，性价比5/5分。\n\n" +
                        "总结：如果你追求性价比，推荐第三款；如果追求品质感，第一款是不错的选择。\n\n" +
                        "你们有用过这些产品吗？欢迎在评论区分享使用感受！";
            }
            case "教程" -> {
                draft = "想挑选到真正好用的高性价比好物？跟着我一步步来！\n\n" +
                        "Step 1: 明确需求\n" +
                        "先想清楚自己最需要什么功能，不要被花哨的宣传迷惑。列出你的核心需求清单。\n\n" +
                        "Step 2: 做功课\n" +
                        "多看真实用户的评价，重点关注中差评，了解产品的真实优缺点。\n\n" +
                        "Step 3: 对比价格\n" +
                        "不要急于下单，多平台对比价格，关注大促活动时机。\n\n" +
                        "Step 4: 关注售后\n" +
                        "选择有保障的品牌和渠道，售后无忧才能安心使用。\n\n" +
                        "按照这个方法，基本上不会踩雷！\n\n" +
                        "有什么挑选好物的小技巧？评论区教教我！";
            }
            case "Vlog" -> {
                draft = "周末睡到自然醒，决定出门逛逛。\n\n" +
                        "最近发现了几样超好用的东西，忍不住想和大家分享。\n\n" +
                        "先去了常逛的那家店，发现上了好多新品。试用了几样，有一款真的让我眼前一亮！\n\n" +
                        "然后又去了隔壁的咖啡馆，点了一杯拿铁，坐在窗边翻看刚买的东西，感觉整个人都被治愈了。\n\n" +
                        "回家路上还在想，生活其实就是由这些小小的美好瞬间组成的。\n\n" +
                        "你们周末一般都怎么过？有没有什么好逛的地方推荐？";
            }
            default -> {
                draft = "姐妹们！！今天分享的东西我真的会反复回购！\n\n" +
                        "最近入手了几样超实用的好物，每一件都是用了就离不开的那种。\n\n" +
                        "第一个是颜值超高的收纳盒，放在桌上瞬间整洁了不少，质感也很好，关键是价格超友好！\n\n" +
                        "第二个是这个小巧的随身好物，出门必备，使用感满分，已经推荐给身边好几个朋友了。\n\n" +
                        "第三个是提升幸福感的小物件，用完之后整个人的心情都变好了，真的强烈推荐！\n\n" +
                        "每一件都是亲测好用的，绝对不踩雷！\n\n" +
                        "你们最近有没有入手什么好物？评论区互相种草呀！";
            }
        }
        return new AiDraftResponse(draft, draft.length());
    }

    private AiCoverRecommendResponse generateMockCover(List<String> imageUrls) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<AiCoverSuggestion> suggestions = imageUrls.stream().map(url -> {
            double composition = 70.0 + random.nextDouble() * 25;
            double color = 70.0 + random.nextDouble() * 25;
            double clarity = 70.0 + random.nextDouble() * 25;
            double score = composition * 0.35 + color * 0.25 + clarity * 0.2 + (80 + random.nextDouble() * 15) * 0.2;
            score = Math.min(100.0, Math.round(score * 10.0) / 10.0);
            return new AiCoverSuggestion(
                    url,
                    score,
                    Math.round(composition * 10.0) / 10.0,
                    Math.round(color * 10.0) / 10.0,
                    Math.round(clarity * 10.0) / 10.0,
                    url + "?crop=3:4"
            );
        }).sorted(Comparator.comparingDouble(AiCoverSuggestion::getScore).reversed())
                .collect(Collectors.toList());

        return new AiCoverRecommendResponse(suggestions);
    }

    private String buildCacheKey(String type, Object... parts) {
        StringBuilder sb = new StringBuilder(CACHE_PREFIX).append(type).append(":");
        for (Object part : parts) {
            if (part != null) {
                sb.append(part.toString().hashCode()).append(":");
            }
        }
        return sb.toString();
    }

    private <T> T getFromCache(String key, Class<T> clazz) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, clazz);
            }
        } catch (Exception e) {
            log.warn("Failed to read cache: key={}", key, e);
        }
        return null;
    }

    private <T> void saveToCache(String key, T data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, cacheTtl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write cache: key={}", key, e);
        }
    }
}