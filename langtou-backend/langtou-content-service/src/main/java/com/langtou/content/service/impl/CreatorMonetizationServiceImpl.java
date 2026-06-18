package com.langtou.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.result.PageResult;
import com.langtou.content.entity.CreatorAdRevenue;
import com.langtou.content.mapper.CreatorAdRevenueMapper;
import com.langtou.content.mapper.CreatorWalletMapper;
import com.langtou.content.service.CreatorMonetizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorMonetizationServiceImpl implements CreatorMonetizationService {

    private final CreatorAdRevenueMapper adRevenueMapper;
    private final CreatorWalletMapper walletMapper;

    /**
     * 曝光单价（元）
     */
    private static final BigDecimal IMPRESSION_UNIT_PRICE = new BigDecimal("0.01");

    /**
     * 点击单价（元）
     */
    private static final BigDecimal CLICK_UNIT_PRICE = new BigDecimal("0.10");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordImpression(Long creatorId, Long noteId) {
        CreatorAdRevenue revenue = new CreatorAdRevenue();
        revenue.setCreatorId(creatorId);
        revenue.setNoteId(noteId);
        revenue.setAdType("IMPRESSION");
        revenue.setImpressions(1);
        revenue.setClicks(0);
        revenue.setCtr(BigDecimal.ZERO);
        revenue.setRevenue(IMPRESSION_UNIT_PRICE);
        revenue.setSettlementStatus("UNSETTLED");

        adRevenueMapper.insert(revenue);
        log.info("记录广告曝光: creatorId={}, noteId={}", creatorId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordClick(Long creatorId, Long noteId) {
        CreatorAdRevenue revenue = new CreatorAdRevenue();
        revenue.setCreatorId(creatorId);
        revenue.setNoteId(noteId);
        revenue.setAdType("CLICK");
        revenue.setImpressions(0);
        revenue.setClicks(1);
        revenue.setCtr(BigDecimal.ZERO);
        revenue.setRevenue(CLICK_UNIT_PRICE);
        revenue.setSettlementStatus("UNSETTLED");

        adRevenueMapper.insert(revenue);
        log.info("记录广告点击: creatorId={}, noteId={}", creatorId, noteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateRevenue(Long creatorId, Long noteId) {
        // 查询该笔记的曝光和点击数据
        QueryWrapper<CreatorAdRevenue> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId)
                .eq("note_id", noteId)
                .eq("settlement_status", "UNSETTLED");
        List<CreatorAdRevenue> records = adRevenueMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return;
        }

        int totalImpressions = 0;
        int totalClicks = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (CreatorAdRevenue record : records) {
            totalImpressions += record.getImpressions() != null ? record.getImpressions() : 0;
            totalClicks += record.getClicks() != null ? record.getClicks() : 0;
            totalRevenue = totalRevenue.add(record.getRevenue() != null ? record.getRevenue() : BigDecimal.ZERO);
        }

        // 计算CTR
        BigDecimal ctr = BigDecimal.ZERO;
        if (totalImpressions > 0) {
            ctr = new BigDecimal(totalClicks)
                    .divide(new BigDecimal(totalImpressions), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        log.info("计算广告收益: creatorId={}, noteId={}, impressions={}, clicks={}, ctr={}%, revenue={}",
                creatorId, noteId, totalImpressions, totalClicks, ctr, totalRevenue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dailySettlement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 查询所有未结算的记录
        QueryWrapper<CreatorAdRevenue> wrapper = new QueryWrapper<>();
        wrapper.eq("settlement_status", "UNSETTLED")
                .lt("created_at", LocalDate.now().atStartOfDay());
        List<CreatorAdRevenue> unsettledRecords = adRevenueMapper.selectList(wrapper);

        if (unsettledRecords.isEmpty()) {
            log.info("日结算: 无未结算记录");
            return;
        }

        // 按创作者汇总
        Map<Long, BigDecimal> revenueMap = new HashMap<>();
        for (CreatorAdRevenue record : unsettledRecords) {
            revenueMap.merge(record.getCreatorId(),
                    record.getRevenue() != null ? record.getRevenue() : BigDecimal.ZERO,
                    BigDecimal::add);
        }

        // 更新结算状态
        for (CreatorAdRevenue record : unsettledRecords) {
            record.setSettlementStatus("SETTLED");
            record.setSettlementDate(yesterday);
            adRevenueMapper.updateById(record);
        }

        // 更新钱包
        for (Map.Entry<Long, BigDecimal> entry : revenueMap.entrySet()) {
            Long creatorId = entry.getKey();
            BigDecimal revenue = entry.getValue();
            var wallet = walletMapper.selectByCreatorId(creatorId);
            if (wallet != null) {
                wallet.setTotalRevenue(wallet.getTotalRevenue().add(revenue));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(revenue));
                walletMapper.updateById(wallet);
            }
        }

        log.info("日结算完成: 共结算{}条记录, 涉及{}位创作者", unsettledRecords.size(), revenueMap.size());
    }

    @Override
    public Map<String, Object> getRevenueOverview(Long creatorId) {
        Map<String, Object> overview = new HashMap<>();
        BigDecimal totalRevenue = adRevenueMapper.sumRevenueByCreator(creatorId);
        BigDecimal unsettledRevenue = adRevenueMapper.sumUnsettledRevenueByCreator(creatorId);
        BigDecimal settledRevenue = totalRevenue.subtract(unsettledRevenue);

        overview.put("totalRevenue", totalRevenue);
        overview.put("unsettledRevenue", unsettledRevenue);
        overview.put("settledRevenue", settledRevenue);
        return overview;
    }

    @Override
    public PageResult<CreatorAdRevenue> getRevenueDetails(Long creatorId, int page, int size) {
        Page<CreatorAdRevenue> pageParam = new Page<>(page, size);
        QueryWrapper<CreatorAdRevenue> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId)
                .orderByDesc("created_at");
        Page<CreatorAdRevenue> result = adRevenueMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public List<Map<String, Object>> getRevenueTrend(Long creatorId, String period) {
        LocalDate now = LocalDate.now();
        String startDate;
        String endDate = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

        switch (period.toLowerCase()) {
            case "week":
                startDate = now.minusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return adRevenueMapper.sumDailyRevenue(creatorId, startDate, endDate);
            case "month":
                startDate = now.minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return adRevenueMapper.sumDailyRevenue(creatorId, startDate, endDate);
            case "year":
                startDate = now.minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return adRevenueMapper.sumMonthlyRevenue(creatorId, startDate, endDate);
            default:
                startDate = now.minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return adRevenueMapper.sumDailyRevenue(creatorId, startDate, endDate);
        }
    }
}
