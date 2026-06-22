package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.content.entity.ContentSimilarity;
import com.langtou.content.entity.DeviceFingerprint;
import com.langtou.content.entity.FraudReport;
import com.langtou.content.mapper.ContentSimilarityMapper;
import com.langtou.content.mapper.DeviceFingerprintMapper;
import com.langtou.content.mapper.FraudReportMapper;
import com.langtou.content.service.AntiFraudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 反作弊服务实现
 *
 * 核心功能：
 * 1. 设备指纹管理 - 记录和追踪设备信息，支持封禁
 * 2. 行为异常检测 - Redis滑动窗口，1分钟内同类操作超过阈值则标记
 * 3. 内容去重检测 - 文本SimHash + 图片感知哈希(pHash)
 * 4. 举报管理 - 创建举报、处理举报
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiFraudServiceImpl implements AntiFraudService {

    private final DeviceFingerprintMapper deviceFingerprintMapper;
    private final FraudReportMapper fraudReportMapper;
    private final ContentSimilarityMapper contentSimilarityMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // ===== 行为检测配置 =====

    /** 行为检测滑动窗口时间（秒） */
    private static final long BEHAVIOR_WINDOW_SECONDS = 60;

    /** 各操作类型的频率阈值（1分钟内最大允许次数） */
    private static final Map<String, Integer> ACTION_THRESHOLD = new HashMap<>();

    /** SimHash相似度阈值（汉明距离 <= 3 视为相似） */
    private static final int SIMHASH_DISTANCE_THRESHOLD = 3;

    /** 内容相似度判定阈值 */
    private static final BigDecimal SIMILARITY_THRESHOLD = new BigDecimal("0.8500");

    static {
        ACTION_THRESHOLD.put("LIKE", 30);
        ACTION_THRESHOLD.put("COMMENT", 15);
        ACTION_THRESHOLD.put("FOLLOW", 20);
        ACTION_THRESHOLD.put("PUBLISH", 10);
        ACTION_THRESHOLD.put("SHARE", 20);
        ACTION_THRESHOLD.put("REPORT", 5);
    }

    // ===== Redis Key =====

    private static final String BEHAVIOR_KEY_PREFIX = "anti_fraud:behavior:";
    private static final String DEVICE_BLOCK_KEY_PREFIX = "anti_fraud:device:blocked:";

    // ==================== 设备指纹管理 ====================

    @Override
    @Transactional
    public DeviceFingerprint recordDeviceFingerprint(Long userId, String deviceId, String deviceBrand,
                                                      String deviceModel, String osType, String osVersion,
                                                      String appVersion, String ipAddress) {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 查询已有设备指纹
        DeviceFingerprint existing = deviceFingerprintMapper.selectByDeviceId(deviceId);

        if (existing != null) {
            // 更新最后活跃时间和其他信息
            existing.setLastSeenAt(LocalDateTime.now());
            if (userId != null) {
                existing.setUserId(userId);
            }
            if (ipAddress != null) {
                existing.setIpAddress(ipAddress);
            }
            if (appVersion != null) {
                existing.setAppVersion(appVersion);
            }
            deviceFingerprintMapper.updateById(existing);
            log.info("更新设备指纹: deviceId={}, userId={}", deviceId, userId);
            return existing;
        }

        // 新建设备指纹
        DeviceFingerprint fingerprint = new DeviceFingerprint();
        fingerprint.setUserId(userId);
        fingerprint.setDeviceId(deviceId);
        fingerprint.setDeviceBrand(deviceBrand);
        fingerprint.setDeviceModel(deviceModel);
        fingerprint.setOsType(osType);
        fingerprint.setOsVersion(osVersion);
        fingerprint.setAppVersion(appVersion);
        fingerprint.setIpAddress(ipAddress);
        fingerprint.setFirstSeenAt(LocalDateTime.now());
        fingerprint.setLastSeenAt(LocalDateTime.now());
        fingerprint.setIsBlocked(0);
        deviceFingerprintMapper.insert(fingerprint);
        log.info("记录新设备指纹: deviceId={}, userId={}, brand={}, model={}",
                deviceId, userId, deviceBrand, deviceModel);
        return fingerprint;
    }

    // ==================== 行为异常检测 ====================

    @Override
    public boolean checkBehaviorAnomaly(Long userId, String actionType) {
        if (userId == null || actionType == null) {
            return false;
        }

        String key = BEHAVIOR_KEY_PREFIX + userId + ":" + actionType;
        int threshold = ACTION_THRESHOLD.getOrDefault(actionType.toUpperCase(), 30);

        // 使用Redis滑动窗口计数
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 首次计数，设置过期时间
            stringRedisTemplate.expire(key, BEHAVIOR_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > threshold) {
            log.warn("行为异常检测触发: userId={}, actionType={}, count={}, threshold={}",
                    userId, actionType, count, threshold);
            return true;
        }

        return false;
    }

    // ==================== 内容去重检测 ====================

    @Override
    @Transactional
    public List<ContentSimilarity> detectDuplicateContent(Long userId, String text, List<String> imageUrls) {
        List<ContentSimilarity> duplicates = new ArrayList<>();

        if (text != null && !text.isEmpty()) {
            // 计算文本SimHash
            long textHash = simHash(text);

            // 查询该用户近期发布的内容进行比对
            // 这里简化处理：将SimHash存入Redis，与历史值比对
            String userTextHashKey = "anti_fraud:text_hash:" + userId;
            List<String> recentHashes = stringRedisTemplate.opsForList().range(userTextHashKey, 0, -1);

            if (recentHashes != null) {
                for (String hashStr : recentHashes) {
                    try {
                        long existingHash = Long.parseLong(hashStr);
                        int distance = hammingDistance(textHash, existingHash);
                        if (distance <= SIMHASH_DISTANCE_THRESHOLD) {
                            // 计算相似度得分
                            BigDecimal similarity = BigDecimal.ONE.subtract(
                                    BigDecimal.valueOf(distance).divide(BigDecimal.valueOf(64), 4, RoundingMode.HALF_UP));
                            if (similarity.compareTo(SIMILARITY_THRESHOLD) >= 0) {
                                log.warn("检测到文本重复: userId={}, similarity={}, distance={}",
                                        userId, similarity, distance);
                                // 返回检测结果（contentId暂为0，实际应关联具体内容）
                                ContentSimilarity cs = new ContentSimilarity();
                                cs.setContentIdA(0L);
                                cs.setContentIdB(0L);
                                cs.setSimilarityScore(similarity);
                                cs.setCheckMethod("TEXT_HASH");
                                cs.setCreatedAt(LocalDateTime.now());
                                duplicates.add(cs);
                            }
                        }
                    } catch (NumberFormatException e) {
                        log.warn("解析历史SimHash失败: {}", hashStr);
                    }
                }
            }

            // 将当前SimHash存入Redis（保留最近100条）
            stringRedisTemplate.opsForList().rightPush(userTextHashKey, String.valueOf(textHash));
            stringRedisTemplate.opsForList().trim(userTextHashKey, -100, -1);
            stringRedisTemplate.expire(userTextHashKey, 7, TimeUnit.DAYS);
        }

        // 图片感知哈希检测（简化实现）
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                // 使用图片URL的hash作为简化pHash（实际应下载图片计算感知哈希）
                long imageHash = md5Hash(imageUrl);
                String userImageHashKey = "anti_fraud:image_hash:" + userId;
                List<String> recentImageHashes = stringRedisTemplate.opsForList().range(userImageHashKey, 0, -1);

                if (recentImageHashes != null) {
                    for (String existingHashStr : recentImageHashes) {
                        try {
                            long existingHash = Long.parseLong(existingHashStr);
                            if (imageHash == existingHash) {
                                log.warn("检测到图片重复: userId={}, imageUrl={}", userId, imageUrl);
                                ContentSimilarity cs = new ContentSimilarity();
                                cs.setContentIdA(0L);
                                cs.setContentIdB(0L);
                                cs.setSimilarityScore(new BigDecimal("1.0000"));
                                cs.setCheckMethod("IMAGE_HASH");
                                cs.setCreatedAt(LocalDateTime.now());
                                duplicates.add(cs);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("解析历史图片Hash失败: {}", existingHashStr);
                        }
                    }
                }

                // 存储图片hash
                String userImageHashKey = "anti_fraud:image_hash:" + userId;
                stringRedisTemplate.opsForList().rightPush(userImageHashKey, String.valueOf(imageHash));
                stringRedisTemplate.opsForList().trim(userImageHashKey, -200, -1);
                stringRedisTemplate.expire(userImageHashKey, 7, TimeUnit.DAYS);
            }
        }

        return duplicates;
    }

    // ==================== 举报管理 ====================

    @Override
    @Transactional
    public FraudReport reportFraud(Long userId, String fraudType, String description, Map<String, Object> evidence) {
        if (userId == null || fraudType == null || fraudType.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 校验fraudType
        List<String> validTypes = Arrays.asList("LIKE_SPAM", "COMMENT_SPAM", "FOLLOW_SPAM",
                "CONTENT_DUPLICATE", "ACCOUNT_ANOMALY");
        if (!validTypes.contains(fraudType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        FraudReport report = new FraudReport();
        report.setUserId(userId);
        report.setFraudType(fraudType);
        report.setSeverity("LOW");
        report.setDescription(description);
        report.setEvidence(evidence);
        report.setStatus("PENDING");
        report.setCreatedAt(LocalDateTime.now());
        fraudReportMapper.insert(report);

        log.info("收到作弊举报: userId={}, fraudType={}, reportId={}", userId, fraudType, report.getId());
        return report;
    }

    @Override
    @Transactional
    public void processFraudReport(Long reportId, String status, Long processorId) {
        if (reportId == null || status == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        List<String> validStatuses = Arrays.asList("CONFIRMED", "DISMISSED");
        if (!validStatuses.contains(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        int rows = fraudReportMapper.processReport(reportId, status, processorId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.REPORT_NOT_FOUND);
        }

        log.info("处理作弊举报: reportId={}, status={}, processorId={}", reportId, status, processorId);
    }

    // ==================== 设备封禁管理 ====================

    @Override
    @Transactional
    public void blockDevice(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }

        // 更新数据库
        deviceFingerprintMapper.blockByDeviceId(deviceId);

        // 更新Redis缓存
        String redisKey = DEVICE_BLOCK_KEY_PREFIX + deviceId;
        stringRedisTemplate.opsForValue().set(redisKey, "1", 365, TimeUnit.DAYS);

        log.warn("设备已封禁: deviceId={}", deviceId);
    }

    @Override
    public boolean isDeviceBlocked(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return false;
        }

        // 先查Redis缓存
        String redisKey = DEVICE_BLOCK_KEY_PREFIX + deviceId;
        String blocked = stringRedisTemplate.opsForValue().get(redisKey);
        if ("1".equals(blocked)) {
            return true;
        }

        // 缓存未命中，查数据库
        Integer isBlocked = deviceFingerprintMapper.checkBlocked(deviceId);
        if (isBlocked != null && isBlocked == 1) {
            // 回填缓存
            stringRedisTemplate.opsForValue().set(redisKey, "1", 365, TimeUnit.DAYS);
            return true;
        }

        return false;
    }

    // ==================== 管理后台接口 ====================

    @Override
    public List<FraudReport> getFraudReports(int page, int size, String status) {
        Page<FraudReport> pageParam = new Page<>(page, size);
        QueryWrapper<FraudReport> wrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        Page<FraudReport> result = fraudReportMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public List<DeviceFingerprint> getDeviceList(int page, int size) {
        Page<DeviceFingerprint> pageParam = new Page<>(page, size);
        QueryWrapper<DeviceFingerprint> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("last_seen_at");
        Page<DeviceFingerprint> result = deviceFingerprintMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 举报总数
        QueryWrapper<FraudReport> totalWrapper = new QueryWrapper<>();
        stats.put("totalReports", fraudReportMapper.selectCount(totalWrapper));

        // 待处理举报数
        QueryWrapper<FraudReport> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("status", "PENDING");
        stats.put("pendingReports", fraudReportMapper.selectCount(pendingWrapper));

        // 已确认举报数
        QueryWrapper<FraudReport> confirmedWrapper = new QueryWrapper<>();
        confirmedWrapper.eq("status", "CONFIRMED");
        stats.put("confirmedReports", fraudReportMapper.selectCount(confirmedWrapper));

        // 已驳回举报数
        QueryWrapper<FraudReport> dismissedWrapper = new QueryWrapper<>();
        dismissedWrapper.eq("status", "DISMISSED");
        stats.put("dismissedReports", fraudReportMapper.selectCount(dismissedWrapper));

        // 封禁设备数
        QueryWrapper<DeviceFingerprint> blockedWrapper = new QueryWrapper<>();
        blockedWrapper.eq("is_blocked", 1);
        stats.put("blockedDevices", deviceFingerprintMapper.selectCount(blockedWrapper));

        // 总设备数
        stats.put("totalDevices", deviceFingerprintMapper.selectCount(new QueryWrapper<>()));

        // 按作弊类型统计
        QueryWrapper<FraudReport> typeGroupWrapper = new QueryWrapper<>();
        typeGroupWrapper.select("fraud_type AS fraudType, COUNT(*) AS count")
                .groupBy("fraud_type");
        List<Map<String, Object>> typeStats = fraudReportMapper.selectMaps(typeGroupWrapper);
        stats.put("fraudTypeDistribution", typeStats);

        return stats;
    }

    // ==================== SimHash 算法实现 ====================

    /**
     * 计算文本的SimHash值
     * 将文本分词后为每个词计算hash，加权合并得到64位SimHash指纹
     *
     * @param text 输入文本
     * @return 64位SimHash值
     */
    private long simHash(String text) {
        // 简单分词：按字符和标点分割
        String[] tokens = text.toLowerCase().split("[\\s,.;!?，。；！？、]+");

        int[] bits = new int[64];

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            long hash = md5Hash(token);
            for (int i = 0; i < 64; i++) {
                if (((hash >> i) & 1) == 1) {
                    bits[i]++;
                } else {
                    bits[i]--;
                }
            }
        }

        // 生成最终SimHash
        long simHash = 0;
        for (int i = 0; i < 64; i++) {
            if (bits[i] > 0) {
                simHash |= (1L << i);
            }
        }
        return simHash;
    }

    /**
     * 计算两个SimHash的汉明距离
     *
     * @param hash1 SimHash值1
     * @param hash2 SimHash值2
     * @return 汉明距离
     */
    private int hammingDistance(long hash1, long hash2) {
        long xor = hash1 ^ hash2;
        int distance = 0;
        while (xor != 0) {
            distance += xor & 1;
            xor >>>= 1;
        }
        return distance;
    }

    /**
     * 计算字符串的MD5哈希（取前8字节作为long）
     *
     * @param input 输入字符串
     * @return hash值
     */
    private long md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash <<= 8;
                hash |= (digest[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            log.error("MD5计算失败", e);
            return input.hashCode();
        }
    }
}
