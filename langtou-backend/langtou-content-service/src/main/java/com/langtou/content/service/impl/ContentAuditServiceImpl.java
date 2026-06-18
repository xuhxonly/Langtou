package com.langtou.content.service.impl;

import com.langtou.content.entity.AuditLog;
import com.langtou.content.entity.Content;
import com.langtou.content.entity.SensitiveWord;
import com.langtou.content.mapper.AuditLogMapper;
import com.langtou.content.mapper.SensitiveWordMapper;
import com.langtou.content.service.ContentAuditService;
import com.langtou.content.service.ImageAuditProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ContentAuditServiceImpl implements ContentAuditService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AuditLogMapper auditLogMapper;
    private final SensitiveWordMapper sensitiveWordMapper;

    /**
     * 敏感词列表（扩展至200+个常见敏感词）
     */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // ===== 违法犯罪 =====
            "赌博", "色情", "毒品", "枪支", "诈骗", "传销",
            "代开发票", "假币", "洗钱", "走私", "暴力", "恐怖",
            "卖淫", "嫖娼", "赌博网站", "博彩", "百家乐",
            "六合彩", "私彩", "套现", "违禁品", "假证",
            "法轮功", "flg", "falun", "反动", "颠覆",
            "杀人", "绑架", "抢劫", "纵火", "投毒",
            "拐卖", "人口贩卖", "非法拘禁", "敲诈勒索",
            "非法集资", "庞氏骗局", "高利贷", "裸贷",
            "校园贷", "套路贷", "砍头息", "暴力催收",
            "黑客", "木马", "钓鱼网站", "网络攻击",
            "洗脑", "邪教", "会道门", "一贯道",
            "偷税", "漏税", "逃税", "抗税",
            "伪造", "变造", "伪造印章", "伪造公文",
            "行贿", "受贿", "贪污", "挪用公款",
            "强奸", "猥亵", "性侵", "性骚扰",
            "聚众斗殴", "寻衅滋事", "故意伤害",
            "非法持有", "非法买卖", "非法运输",
            "盗版", "侵权", "假冒伪劣",

            // ===== 色情低俗 =====
            "色情服务", "招嫖", "约炮", "一夜情",
            "裸聊", "色诱", "性交易", "援交",
            "成人网站", "黄色网站", "AV", "毛片",
            "三级片", "情色", "风月", "春药",
            "催情", "迷药", "迷奸", "下药",
            "偷拍", "裙底", "走光", "露点",
            "福利姬", "私房照", "大尺度",
            "黄播", "色播", "裸播",

            // ===== 赌博相关 =====
            "赌场", "赌资", "赌局", "赌注", "赌客",
            "押注", "下注", "庄家", "赔率", "盘口",
            "老虎机", "轮盘", "21点", "德州扑克",
            "炸金花", "斗地主赌博", "棋牌赌博",
            "网络赌博", "线上赌博", "体育博彩",
            "赌球", "赌马", "赛狗", "彩票预测",
            "内部消息", "稳赚不赔", "包赚",

            // ===== 毒品相关 =====
            "冰毒", "海洛因", "大麻", "可卡因",
            "摇头丸", "K粉", "麻古", "鸦片",
            "吗啡", "杜冷丁", "安非他命", "甲基苯丙胺",
            "吸毒", "贩毒", "制毒", "容留他人吸毒",
            "吸毒工具", "毒品交易", "毒友",
            "电子烟", "上头电子烟", "依托咪酯",

            // ===== 枪支武器 =====
            "枪械", "弹药", "子弹", "手枪", "步枪",
            "猎枪", "气枪", "仿真枪", "水弹枪",
            "炸药", "雷管", "导火索", "TNT",
            "管制刀具", "弩", "弓箭", "枪支配件",
            "3D打印枪", "自制枪", "枪支改装",

            // ===== 诈骗相关 =====
            "杀猪盘", "刷单", "刷信誉", "兼职诈骗",
            "退款诈骗", "冒充客服", "冒充公检法",
            "网络贷款诈骗", "投资诈骗", "理财诈骗",
            "虚假广告", "虚假宣传", "钓鱼链接",
            "中奖诈骗", "补贴诈骗", "助学金诈骗",
            "冒充领导", "冒充熟人", "猜猜我是谁",
            "裸聊敲诈", "仙人跳", "酒托", "茶托",

            // ===== 政治敏感 =====
            "分裂国家", "煽动颠覆", "危害国家安全",
            "泄露国家秘密", "间谍", "特工",
            "恐怖组织", "恐怖袭击", "极端主义",
            "暴恐", "圣战", "殉道",

            // ===== 虚假信息 =====
            "代孕", "卖血", "器官买卖", "人口倒卖",
            "非法移民", "偷渡", "蛇头",
            "假药", "劣药", "过期药品",
            "地沟油", "毒奶粉", "假疫苗",
            "传销组织", "拉人头", "入门费",

            // ===== 其他违规 =====
            "代写论文", "代考", "替考", "考试作弊",
            "买卖驾照", "买卖身份证", "买卖银行卡",
            "四件套", "对公账户", "跑分",
            "虚拟货币洗钱", "USDT洗钱", "泰达币",
            "帮信罪", "掩隐罪",
            "网络水军", "刷量", "控评", "删帖",
            "恶意差评", "职业打假", "敲诈商家",
            "人肉搜索", "网络暴力", "网暴",
            "恶意举报", "诬告陷害",
            "非法行医", "庸医", "假医生",
            "高仿", "A货", "复刻", "原单",
            "私服", "外挂", "作弊器", "脚本",
            "封号", "解封", "租号", "卖号"
    );

    /**
     * 谐音/拼音映射表
     */
    private static final Map<String, List<String>> HOMOPHONE_MAP = new HashMap<>();

    static {
        HOMOPHONE_MAP.put("法", Arrays.asList("fa", "发", "罚"));
        HOMOPHONE_MAP.put("轮", Arrays.asList("lun", "伦", "仑"));
        HOMOPHONE_MAP.put("功", Arrays.asList("gong", "公", "工"));
        HOMOPHONE_MAP.put("赌", Arrays.asList("du", "睹"));
        HOMOPHONE_MAP.put("博", Arrays.asList("bo", "搏", "伯"));
        HOMOPHONE_MAP.put("毒", Arrays.asList("du", "独", "读"));
        HOMOPHONE_MAP.put("枪", Arrays.asList("qiang", "强", "墙"));
        HOMOPHONE_MAP.put("色", Arrays.asList("se", "涩"));
        HOMOPHONE_MAP.put("嫖", Arrays.asList("piao", "飘"));
        HOMOPHONE_MAP.put("淫", Arrays.asList("yin", "银", "寅"));
        HOMOPHONE_MAP.put("贩", Arrays.asList("fan", "贩", "翻"));
        HOMOPHONE_MAP.put("骗", Arrays.asList("pian", "片", "偏"));
        HOMOPHONE_MAP.put("炸", Arrays.asList("zha", "诈"));
        HOMOPHONE_MAP.put("杀", Arrays.asList("sha", "沙", "纱"));
        HOMOPHONE_MAP.put("毒", Arrays.asList("du", "度", "渡"));
    }

    /**
     * 图片审核服务提供者（支持运行时切换）
     */
    private ImageAuditProvider imageAuditProvider;

    /**
     * 图片审核缓存前缀（Redis）
     */
    private static final String IMAGE_AUDIT_CACHE_PREFIX = "audit:image:md5:";
    private static final int IMAGE_AUDIT_CACHE_HOURS = 72; // 审核通过缓存72小时

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

    /**
     * 拆字检测用的分隔符正则
     */
    private static final Pattern SPLIT_CHAR_PATTERN = Pattern.compile("[_\\-\\s\\u00B7\\u2022]+");

    public ContentAuditServiceImpl(StringRedisTemplate stringRedisTemplate, AuditLogMapper auditLogMapper, SensitiveWordMapper sensitiveWordMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.auditLogMapper = auditLogMapper;
        this.sensitiveWordMapper = sensitiveWordMapper;
        // 默认使用本地内置审核
        this.imageAuditProvider = new LocalImageAuditProvider();
    }

    /**
     * 设置图片审核服务提供者（支持运行时切换）
     */
    public void setImageAuditProvider(ImageAuditProvider provider) {
        if (provider != null && provider.isAvailable()) {
            log.info("切换图片审核服务商: {}", provider.getProviderName());
            this.imageAuditProvider = provider;
        }
    }

    @Override
    public boolean checkContent(Content content, Long userId) {
        // 1. 敏感词过滤（支持拼音、拆字、谐音检测）
        if (containsSensitiveWords(content.getTitle())) {
            log.warn("标题包含敏感词: userId={}, title={}", userId, content.getTitle());
            recordAuditLog("text", content.getId(), userId, content.getTitle(), "reject", "标题包含敏感词", "local", null, 0L);
            return false;
        }
        if (StringUtils.hasText(content.getContent()) && containsSensitiveWords(content.getContent())) {
            log.warn("内容包含敏感词: userId={}, content={}", userId, content.getContent());
            recordAuditLog("text", content.getId(), userId,
                    content.getContent().length() > 200 ? content.getContent().substring(0, 200) : content.getContent(),
                    "reject", "内容包含敏感词", "local", null, 0L);
            return false;
        }

        // 2. 图片审核
        if (StringUtils.hasText(content.getImages())) {
            String[] imageUrls = content.getImages().split(",");
            for (String imageUrl : imageUrls) {
                imageUrl = imageUrl.trim();
                if (StringUtils.hasText(imageUrl)) {
                    if (!auditImageWithCache(imageUrl, content.getId(), userId)) {
                        log.warn("图片审核未通过: userId={}, imageUrl={}", userId, imageUrl);
                        return false;
                    }
                }
            }
        }

        // 3. 图片数量限制
        if (StringUtils.hasText(content.getImages())) {
            int imageCount = content.getImages().split(",").length;
            if (imageCount > MAX_IMAGE_COUNT) {
                log.warn("图片数量超限: userId={}, count={}, max={}", userId, imageCount, MAX_IMAGE_COUNT);
                return false;
            }
        }

        // 4. 内容长度限制
        if (StringUtils.hasText(content.getTitle()) && content.getTitle().length() > MAX_TITLE_LENGTH) {
            log.warn("标题长度超限: userId={}, length={}, max={}", userId, content.getTitle().length(), MAX_TITLE_LENGTH);
            return false;
        }
        if (StringUtils.hasText(content.getContent()) && content.getContent().length() > MAX_CONTENT_LENGTH) {
            log.warn("内容长度超限: userId={}, length={}, max={}", userId, content.getContent().length(), MAX_CONTENT_LENGTH);
            return false;
        }

        // 5. 新用户发布频率限制（使用Redis计数器）
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

        // 1. 原始文本检测
        String normalizedText = normalizeText(text);
        for (String word : SENSITIVE_WORDS) {
            if (normalizedText.contains(word.toLowerCase())) {
                return true;
            }
        }

        // 2. 拼音检测
        if (containsPinyinSensitiveWords(text)) {
            return true;
        }

        // 3. 拆字检测（如"法_轮_功"）
        if (containsSplitCharSensitiveWords(text)) {
            return true;
        }

        // 4. 谐音检测
        if (containsHomophoneSensitiveWords(text)) {
            return true;
        }

        return false;
    }

    /**
     * 带缓存的图片审核
     * 1. 先计算图片MD5
     * 2. 查询Redis缓存，审核通过的图片直接跳过
     * 3. 查询数据库审核日志，近期通过的图片也跳过
     * 4. 调用审核服务提供者进行审核
     * 5. 审核通过则写入缓存
     */
    private boolean auditImageWithCache(String imageUrl, Long targetId, Long userId) {
        if (!StringUtils.hasText(imageUrl)) {
            return true;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 计算图片MD5
            String imageMd5 = computeImageMd5(imageUrl);

            // 1. 查询Redis缓存
            String cacheKey = IMAGE_AUDIT_CACHE_PREFIX + imageMd5;
            String cachedResult = stringRedisTemplate.opsForValue().get(cacheKey);
            if ("pass".equals(cachedResult)) {
                log.debug("图片审核命中Redis缓存(通过): imageUrl={}, md5={}", imageUrl, imageMd5);
                return true;
            }
            if ("reject".equals(cachedResult)) {
                log.debug("图片审核命中Redis缓存(拒绝): imageUrl={}, md5={}", imageUrl, imageMd5);
                return false;
            }

            // 2. 查询数据库审核日志（缓存未命中时查DB）
            if (StringUtils.hasText(imageMd5)) {
                AuditLog dbLog = auditLogMapper.findLatestPassByImageMd5(imageMd5);
                if (dbLog != null) {
                    // 数据库有审核通过记录，写入Redis缓存并直接返回
                    stringRedisTemplate.opsForValue().set(cacheKey, "pass", IMAGE_AUDIT_CACHE_HOURS, TimeUnit.HOURS);
                    log.debug("图片审核命中数据库缓存(通过): imageUrl={}, md5={}", imageUrl, imageMd5);
                    return true;
                }
            }

            // 3. 调用审核服务
            boolean result;
            String providerName;
            String reason = null;

            if (imageAuditProvider != null && imageAuditProvider.isAvailable()) {
                result = imageAuditProvider.auditImage(imageUrl);
                providerName = imageAuditProvider.getProviderName();
            } else {
                // 无可用审核服务，使用本地MD5缓存策略：首次通过则缓存
                log.warn("无可用图片审核服务，使用本地默认策略: imageUrl={}", imageUrl);
                result = true;
                providerName = "local_fallback";
            }

            long durationMs = System.currentTimeMillis() - startTime;

            // 4. 记录审核日志到数据库
            recordAuditLog(
                    "image", targetId, userId,
                    imageUrl.length() > 200 ? imageUrl.substring(0, 200) : imageUrl,
                    result ? "pass" : "reject",
                    reason,
                    providerName,
                    imageMd5,
                    durationMs
            );

            // 5. 审核通过则写入Redis缓存
            if (result) {
                stringRedisTemplate.opsForValue().set(cacheKey, "pass", IMAGE_AUDIT_CACHE_HOURS, TimeUnit.HOURS);
            } else {
                // 拒绝的图片缓存较短时间，避免重复调用
                stringRedisTemplate.opsForValue().set(cacheKey, "reject", 1, TimeUnit.HOURS);
            }

            return result;

        } catch (Exception e) {
            log.error("图片审核异常: imageUrl={}, error={}", imageUrl, e.getMessage());
            // 异常时记录审核日志
            long durationMs = System.currentTimeMillis() - startTime;
            recordAuditLog(
                    "image", targetId, userId,
                    imageUrl.length() > 200 ? imageUrl.substring(0, 200) : imageUrl,
                    "reject",
                    "审核异常: " + e.getMessage(),
                    imageAuditProvider != null ? imageAuditProvider.getProviderName() : "unknown",
                    null,
                    durationMs
            );
            // 异常时默认不通过
            return false;
        }
    }

    /**
     * 计算图片MD5（通过URL下载并计算）
     */
    private String computeImageMd5(String imageUrl) {
        try {
            InputStream inputStream = new URL(imageUrl).openStream();
            return DigestUtils.md5DigestAsHex(inputStream);
        } catch (Exception e) {
            log.warn("计算图片MD5失败: imageUrl={}, error={}", imageUrl, e.getMessage());
            // 使用URL本身的hash作为fallback
            return DigestUtils.md5DigestAsHex(imageUrl.getBytes());
        }
    }

    /**
     * 记录审核日志到数据库
     */
    private void recordAuditLog(String auditType, Long targetId, Long userId,
                                 String content, String result, String reason,
                                 String provider, String imageMd5, Long durationMs) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAuditType(auditType);
            auditLog.setTargetId(targetId);
            auditLog.setUserId(userId);
            auditLog.setContent(content);
            auditLog.setResult(result);
            auditLog.setReason(reason);
            auditLog.setProvider(provider);
            auditLog.setImageMd5(imageMd5);
            auditLog.setDurationMs(durationMs);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("记录审核日志失败: error={}", e.getMessage());
        }
    }

    /**
     * 文本归一化：转小写、去除多余空格
     */
    private String normalizeText(String text) {
        return text.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * 拼音检测
     * 检测如 "fa lun gong" 等拼音形式的敏感词
     */
    private boolean containsPinyinSensitiveWords(String text) {
        String normalized = text.toLowerCase();
        // 检测拼音形式的敏感词
        List<String> pinyinPatterns = Arrays.asList(
                "fa\\s*lun\\s*gong",
                "du\\s*bo",
                "du\\s*pin",
                "qiang\\s*zhi",
                "se\\s*qing",
                "dai\\s*kai\\s*fa\\s*piao",
                "mai\\s*yin\\s*mai\\s*chun",
                "bing\\s*du",
                "hai\\s*luo\\s*yin",
                "da\\s*ma",
                "yao\\s*tou\\s*wan",
                "sha\\s*zhu\\s*pan",
                "shua\\s*dan",
                "tuo\\s*yun"
        );
        for (String pattern : pinyinPatterns) {
            if (Pattern.compile(pattern).matcher(normalized).find()) {
                log.debug("检测到拼音敏感词: pattern={}", pattern);
                return true;
            }
        }
        return false;
    }

    /**
     * 拆字检测
     * 检测如 "法_轮_功"、"法-轮-功"、"法 轮 功" 等拆字形式
     */
    private boolean containsSplitCharSensitiveWords(String text) {
        // 去除拆字分隔符后检测
        String cleaned = SPLIT_CHAR_PATTERN.matcher(text).replaceAll("");
        String normalized = normalizeText(cleaned);
        for (String word : SENSITIVE_WORDS) {
            if (normalized.contains(word.toLowerCase())) {
                log.debug("检测到拆字敏感词: original={}, cleaned={}", text, cleaned);
                return true;
            }
        }
        return false;
    }

    /**
     * 谐音检测
     * 检测谐音替换的敏感词
     */
    private boolean containsHomophoneSensitiveWords(String text) {
        String normalized = normalizeText(text);
        for (Map.Entry<String, List<String>> entry : HOMOPHONE_MAP.entrySet()) {
            String originalChar = entry.getKey();
            List<String> alternatives = entry.getValue();
            // 检查是否包含原字或谐音字
            for (String alt : alternatives) {
                // 构建可能的谐音组合进行模糊匹配
                String pattern = buildHomophonePattern(originalChar, alternatives);
                if (Pattern.compile(pattern).matcher(normalized).find()) {
                    log.debug("检测到谐音敏感词: text={}, pattern={}", text, pattern);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 构建谐音正则表达式
     */
    private String buildHomophonePattern(String originalChar, List<String> alternatives) {
        StringBuilder sb = new StringBuilder("(");
        sb.append(originalChar);
        for (String alt : alternatives) {
            sb.append("|").append(alt);
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 图片审核（调用第三方审核服务占位接口）
     * 保留向后兼容，内部委托给带缓存的审核方法
     *
     * @param imageUrl 图片URL
     * @return 审核结果：true通过，false不通过
     */
    public boolean auditImage(String imageUrl) {
        return auditImageWithCache(imageUrl, null, null);
    }

    /**
     * 批量图片审核
     */
    public Map<String, Boolean> batchAuditImages(List<String> imageUrls) {
        Map<String, Boolean> results = new HashMap<>();
        for (String url : imageUrls) {
            results.put(url, auditImage(url));
        }
        return results;
    }

    @Override
    public List<String> getSensitiveWords() {
        // 优先从数据库读取启用的敏感词，若表不存在则回退到内置列表
        try {
            List<String> dbWords = sensitiveWordMapper.selectAllEnabledWords();
            if (dbWords != null && !dbWords.isEmpty()) {
                // 合并内置词库与数据库词库（去重）
                Set<String> merged = new LinkedHashSet<>(SENSITIVE_WORDS);
                merged.addAll(dbWords);
                return new ArrayList<>(merged);
            }
        } catch (Exception e) {
            log.warn("从数据库读取敏感词失败，使用内置词库: {}", e.getMessage());
        }
        return new ArrayList<>(SENSITIVE_WORDS);
    }

    @Override
    public void addSensitiveWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            String trimmed = word.trim();
            log.info("添加敏感词: {}", trimmed);
            try {
                SensitiveWord existing = sensitiveWordMapper.selectByWord(trimmed);
                if (existing != null) {
                    // 已存在则启用
                    if (!"ENABLED".equals(existing.getStatus())) {
                        existing.setStatus("ENABLED");
                        sensitiveWordMapper.updateById(existing);
                    }
                } else {
                    SensitiveWord sw = new SensitiveWord();
                    sw.setWord(trimmed);
                    sw.setCategory("custom");
                    sw.setSource("CUSTOM");
                    sw.setStatus("ENABLED");
                    sensitiveWordMapper.insert(sw);
                }
            } catch (Exception e) {
                log.error("添加敏感词到数据库失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public void removeSensitiveWord(Long wordId) {
        log.info("删除敏感词: wordId={}", wordId);
        try {
            sensitiveWordMapper.deleteById(wordId);
        } catch (Exception e) {
            log.error("从数据库删除敏感词失败: {}", e.getMessage());
        }
    }

    // ==================== AI 内容审核（阿里云/腾讯云预留接入点）====================

    /**
     * AI 图像审核（预留接入点）
     * 返回详细审核结果，支持后续接入阿里云/腾讯云内容安全 API
     */
    public AuditResult auditImageForResult(String imageUrl) {
        // TODO: 接入阿里云/腾讯云内容安全 API
        // 模拟实现：默认通过
        return AuditResult.builder()
                .status(AuditResult.STATUS_PASS)
                .reason("AI图像审核通过")
                .confidence(0.99)
                .provider(aiAuditEnabled ? aiProvider : "local")
                .build();
    }

    /**
     * AI 视频审核（关键帧审核）
     * 返回详细审核结果，支持后续接入视频审核 API
     */
    public AuditResult auditVideoForResult(String videoUrl) {
        // TODO: 接入视频审核 API，提取关键帧进行审核
        return AuditResult.builder()
                .status(AuditResult.STATUS_PASS)
                .reason("AI视频审核通过")
                .confidence(0.99)
                .provider(aiAuditEnabled ? aiProvider : "local")
                .build();
    }

    /**
     * AI 文本审核（增强现有敏感词）
     * 先走敏感词检测，再走 AI 审核
     */
    public AuditResult auditTextForResult(String text) {
        // 先走敏感词检测
        boolean hasSensitive = containsSensitiveWords(text);
        if (hasSensitive) {
            return AuditResult.builder()
                    .status(AuditResult.STATUS_REJECT)
                    .reason("包含敏感词")
                    .confidence(1.0)
                    .provider("local")
                    .build();
        }
        // TODO: 接入文本审核 API
        return AuditResult.builder()
                .status(AuditResult.STATUS_PASS)
                .reason("AI文本审核通过")
                .confidence(0.95)
                .provider(aiAuditEnabled ? aiProvider : "local")
                .build();
    }

    @Value("${langtou.audit.ai.enabled:false}")
    private boolean aiAuditEnabled;

    @Value("${langtou.audit.ai.provider:aliyun}")
    private String aiProvider;

    @Value("${langtou.audit.ai.endpoint:}")
    private String aiAuditEndpoint;

    @Value("${langtou.audit.ai.accessKey:}")
    private String aiAccessKey;

    @Value("${langtou.audit.ai.secretKey:}")
    private String aiSecretKey;

    /**
     * AI 内容审核（文本 + 图片 + 视频）
     * 优先调用第三方 AI 审核服务，未配置时 fallback 到本地敏感词审核
     */
    @Override
    public AuditResult aiAuditContent(Content content, Long userId) {
        long startTime = System.currentTimeMillis();

        // 1. 文本 AI 审核
        AuditResult textResult = aiAuditText(content.getTitle() + " " + content.getContent());
        if (textResult.isReject()) {
            recordAuditLog("ai_text", content.getId(), userId,
                    content.getTitle(), "reject", textResult.getReason(),
                    textResult.getProvider(), null, System.currentTimeMillis() - startTime);
            return textResult;
        }

        // 2. 图片 AI 审核
        if (StringUtils.hasText(content.getImages())) {
            String[] imageUrls = content.getImages().split(",");
            for (String imageUrl : imageUrls) {
                imageUrl = imageUrl.trim();
                if (!StringUtils.hasText(imageUrl)) {
                    continue;
                }
                AuditResult imageResult = aiAuditImage(imageUrl);
                if (imageResult.isReject()) {
                    recordAuditLog("ai_image", content.getId(), userId,
                            imageUrl, "reject", imageResult.getReason(),
                            imageResult.getProvider(), null, System.currentTimeMillis() - startTime);
                    return imageResult;
                }
                if (imageResult.isReview()) {
                    recordAuditLog("ai_image", content.getId(), userId,
                            imageUrl, "review", imageResult.getReason(),
                            imageResult.getProvider(), null, System.currentTimeMillis() - startTime);
                    return imageResult;
                }
            }
        }

        // 3. 视频 AI 审核
        if (StringUtils.hasText(content.getVideoUrl())) {
            AuditResult videoResult = aiAuditVideo(content.getVideoUrl());
            if (videoResult.isReject() || videoResult.isReview()) {
                recordAuditLog("ai_video", content.getId(), userId,
                        content.getVideoUrl(), videoResult.isReject() ? "reject" : "review",
                        videoResult.getReason(), videoResult.getProvider(), null,
                        System.currentTimeMillis() - startTime);
                return videoResult;
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        AuditResult passResult = AuditResult.pass(aiAuditEnabled ? aiProvider : "local");
        passResult.setDurationMs(durationMs);
        return passResult;
    }

    /**
     * AI 文本审核
     * 模拟调用阿里云/腾讯云内容安全 API，实际接入时替换为真实 HTTP 调用
     */
    @Override
    public AuditResult aiAuditImage(String imageUrl) {
        long startTime = System.currentTimeMillis();

        if (!aiAuditEnabled) {
            log.warn("AI 审核未启用，图片默认通过: imageUrl={}", imageUrl);
            return AuditResult.pass("local_fallback");
        }

        try {
            // TODO: 生产环境替换为真实 AI 审核 API 调用
            // 阿里云内容安全示例：
            // String apiUrl = "https://green.aliyuncs.com/v2/image/syncscan";
            // 腾讯云内容安全示例：
            // String apiUrl = "https://ims.tencentcloudapi.com/?Action=ImageModeration";

            // 模拟调用结构（预留接入点）
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("imageUrl", imageUrl);
            requestBody.put("scenes", Arrays.asList("porn", "terrorism", "ad", "politics"));

            log.info("[AI Audit] 调用图片审核服务: provider={}, imageUrl={}", aiProvider, imageUrl);

            // 模拟审核结果（实际接入时替换为真实 HTTP 请求）
            // HttpHeaders headers = buildAiHeaders();
            // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            // ResponseEntity<String> response = restTemplate.postForEntity(aiAuditEndpoint, entity, String.class);

            // 模拟：95% 概率通过，3% 概率人工复核，2% 概率拒绝
            double random = Math.random();
            long durationMs = System.currentTimeMillis() - startTime;

            if (random > 0.98) {
                AuditResult result = AuditResult.reject("检测到违规内容（模拟）", aiProvider);
                result.setRiskLabels(Arrays.asList("porn"));
                result.setConfidence(0.95);
                result.setDurationMs(durationMs);
                return result;
            } else if (random > 0.95) {
                AuditResult result = AuditResult.review("疑似违规，建议人工复核", Arrays.asList("ad"), aiProvider);
                result.setConfidence(0.72);
                result.setDurationMs(durationMs);
                return result;
            }

            AuditResult passResult = AuditResult.pass(aiProvider);
            passResult.setDurationMs(durationMs);
            return passResult;

        } catch (Exception e) {
            log.error("AI 图片审核异常: imageUrl={}, error={}", imageUrl, e.getMessage());
            return AuditResult.review("审核服务异常，转入人工复核", Arrays.asList("system_error"), "local");
        }
    }

    /**
     * AI 视频审核
     * 模拟调用阿里云/腾讯云视频审核 API
     */
    @Override
    public AuditResult aiAuditVideo(String videoUrl) {
        long startTime = System.currentTimeMillis();

        if (!aiAuditEnabled) {
            log.warn("AI 审核未启用，视频默认通过: videoUrl={}", videoUrl);
            return AuditResult.pass("local_fallback");
        }

        try {
            log.info("[AI Audit] 调用视频审核服务: provider={}, videoUrl={}", aiProvider, videoUrl);

            // TODO: 生产环境替换为真实视频审核 API
            // 视频审核通常为异步任务，需要轮询结果
            // 阿里云：CreateVideoModerationTask + GetVideoModerationResult
            // 腾讯云：CreateVideoModerationTask + DescribeTaskDetail

            long durationMs = System.currentTimeMillis() - startTime;
            AuditResult passResult = AuditResult.pass(aiProvider);
            passResult.setDurationMs(durationMs);
            return passResult;

        } catch (Exception e) {
            log.error("AI 视频审核异常: videoUrl={}, error={}", videoUrl, e.getMessage());
            return AuditResult.review("视频审核服务异常，转入人工复核", Arrays.asList("system_error"), "local");
        }
    }

    /**
     * 批量 AI 图片审核
     */
    @Override
    public Map<String, AuditResult> batchAiAuditImages(List<String> imageUrls) {
        Map<String, AuditResult> results = new HashMap<>();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return results;
        }
        for (String url : imageUrls) {
            results.put(url, aiAuditImage(url));
        }
        return results;
    }

    /**
     * AI 文本审核（内部方法）
     */
    private AuditResult aiAuditText(String text) {
        long startTime = System.currentTimeMillis();

        if (!aiAuditEnabled) {
            // AI 未启用时，fallback 到本地敏感词检测
            boolean hasSensitive = containsSensitiveWords(text);
            if (hasSensitive) {
                return AuditResult.reject("包含敏感词", "local");
            }
            return AuditResult.pass("local");
        }

        try {
            log.info("[AI Audit] 调用文本审核服务: provider={}, textLength={}", aiProvider, text != null ? text.length() : 0);

            // TODO: 生产环境替换为真实文本审核 API
            // 阿里云：/v2/text/antispam
            // 腾讯云：TextModeration

            long durationMs = System.currentTimeMillis() - startTime;
            AuditResult passResult = AuditResult.pass(aiProvider);
            passResult.setDurationMs(durationMs);
            return passResult;

        } catch (Exception e) {
            log.error("AI 文本审核异常: error={}", e.getMessage());
            // 异常时 fallback 到本地敏感词检测
            boolean hasSensitive = containsSensitiveWords(text);
            if (hasSensitive) {
                return AuditResult.reject("包含敏感词", "local");
            }
            return AuditResult.pass("local");
        }
    }

    /**
     * 构建 AI 审核请求头（预留）
     */
    private HttpHeaders buildAiHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // TODO: 根据实际服务商添加签名
        // 阿里云：Authorization = "acs " + accessKey + ":" + signature
        // 腾讯云：X-TC-Action, X-TC-Version, X-TC-Timestamp, Authorization = TC3-HMAC-SHA256 ...
        if (StringUtils.hasText(aiAccessKey)) {
            headers.set("X-AI-Access-Key", aiAccessKey);
        }
        return headers;
    }

    // ==================== 本地图片审核提供者（默认实现）====================

    /**
     * 本地图片审核提供者（默认实现，无第三方服务时使用）
     */
    static class LocalImageAuditProvider implements ImageAuditProvider {

        @Override
        public boolean auditImage(String imageUrl) {
            // 本地审核仅做基本检查，不进行深度AI审核
            // 生产环境应替换为阿里云/腾讯云/百度AI等审核服务
            log.info("[LocalAudit] 本地图片审核(默认通过): imageUrl={}", imageUrl);
            return true;
        }

        @Override
        public String getProviderName() {
            return "local";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }
}
