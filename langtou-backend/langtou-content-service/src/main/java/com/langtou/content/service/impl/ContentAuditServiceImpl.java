package com.langtou.content.service.impl;

import com.langtou.content.entity.Content;
import com.langtou.content.service.ContentAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ContentAuditServiceImpl implements ContentAuditService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 敏感词列表（MVP阶段基础词库，生产环境应接入专业审核服务）
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "赌博", "色情", "毒品", "枪支", "诈骗", "传销",
            "代开发票", "假币", "洗钱", "走私", "暴力", "恐怖",
            "卖淫", "嫖娼", "赌博网站", "博彩", "百家乐",
            "六合彩", "私彩", "套现", "违禁品", "假证"
    );

    /**
     * 最大图片数量
     */
    private static final int MAX_IMAGE_COUNT = 18;

    /**
     * 标题最大长度
     */
    private static final int MAX_TITLE_LENGTH = 100;

    /**
     * 内容最大长度
     */
    private static final int MAX_CONTENT_LENGTH = 10000;

    /**
     * 新用户发布频率限制：每小时最多发布数
     */
    private static final int PUBLISH_RATE_LIMIT = 10;

    /**
     * 发布频率限制时间窗口（小时）
     */
    private static final int PUBLISH_RATE_WINDOW_HOURS = 1;

    private static final String PUBLISH_RATE_PREFIX = "publish:rate:";

    public ContentAuditServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean checkContent(Content content, Long userId) {
        // 1. 敏感词过滤
        if (containsSensitiveWords(content.getTitle())) {
            log.warn("标题包含敏感词: userId={}, title={}", userId, content.getTitle());
            return false;
        }
        if (StringUtils.hasText(content.getContent()) && containsSensitiveWords(content.getContent())) {
            log.warn("内容包含敏感词: userId={}, content={}", userId, content.getContent());
            return false;
        }

        // 2. 图片数量限制
        if (StringUtils.hasText(content.getImages())) {
            int imageCount = content.getImages().split(",").length;
            if (imageCount > MAX_IMAGE_COUNT) {
                log.warn("图片数量超限: userId={}, count={}, max={}", userId, imageCount, MAX_IMAGE_COUNT);
                return false;
            }
        }

        // 3. 内容长度限制
        if (StringUtils.hasText(content.getTitle()) && content.getTitle().length() > MAX_TITLE_LENGTH) {
            log.warn("标题长度超限: userId={}, length={}, max={}", userId, content.getTitle().length(), MAX_TITLE_LENGTH);
            return false;
        }
        if (StringUtils.hasText(content.getContent()) && content.getContent().length() > MAX_CONTENT_LENGTH) {
            log.warn("内容长度超限: userId={}, length={}, max={}", userId, content.getContent().length(), MAX_CONTENT_LENGTH);
            return false;
        }

        // 4. 新用户发布频率限制（使用Redis计数器）
        String rateKey = PUBLISH_RATE_PREFIX + userId;
        String countStr = stringRedisTemplate.opsForValue().get(rateKey);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;
        if (count >= PUBLISH_RATE_LIMIT) {
            log.warn("发布频率超限: userId={}, count={}", userId, count);
            return false;
        }

        // 增加计数器
        if (count == 0) {
            stringRedisTemplate.opsForValue().set(rateKey, "1", PUBLISH_RATE_WINDOW_HOURS, TimeUnit.HOURS);
        } else {
            stringRedisTemplate.opsForValue().increment(rateKey);
        }

        return true;
    }

    @Override
    public boolean containsSensitiveWords(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
