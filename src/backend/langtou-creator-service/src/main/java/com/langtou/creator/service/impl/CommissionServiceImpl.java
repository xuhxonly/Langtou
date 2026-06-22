package com.langtou.creator.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.PageResult;
import com.langtou.creator.entity.CreatorCommission;
import com.langtou.creator.mapper.CreatorCommissionMapper;
import com.langtou.creator.service.CommissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CreatorCommissionMapper commissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorCommission recordCommission(Long creatorId, Long productId, Long noteId,
                                              String orderNo, Long buyerId,
                                              BigDecimal amount, BigDecimal commissionAmount) {
        CreatorCommission commission = new CreatorCommission();
        commission.setCreatorId(creatorId);
        commission.setProductId(productId);
        commission.setNoteId(noteId);
        commission.setOrderNo(orderNo);
        commission.setBuyerId(buyerId);
        commission.setAmount(amount);
        commission.setCommissionAmount(commissionAmount);
        commission.setStatus("PENDING");

        commissionMapper.insert(commission);
        log.info("佣金记录创建: creatorId={}, productId={}, commissionAmount={}", creatorId, productId, commissionAmount);
        return commission;
    }

    @Override
    public PageResult<CreatorCommission> getCommissionList(Long creatorId, int page, int size) {
        Page<CreatorCommission> pageParam = new Page<>(page, size);
        QueryWrapper<CreatorCommission> wrapper = new QueryWrapper<>();
        wrapper.eq("creator_id", creatorId).orderByDesc("created_at");
        Page<CreatorCommission> result = commissionMapper.selectPage(pageParam, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Map<String, Object> getCommissionSummary(Long creatorId) {
        Map<String, Object> summary = new HashMap<>();
        BigDecimal totalCommission = commissionMapper.sumCommissionByCreator(creatorId);
        BigDecimal pendingCommission = commissionMapper.sumCommissionByCreatorAndStatus(creatorId, "PENDING");
        BigDecimal settledCommission = commissionMapper.sumCommissionByCreatorAndStatus(creatorId, "SETTLED");
        BigDecimal refundedCommission = commissionMapper.sumCommissionByCreatorAndStatus(creatorId, "REFUNDED");

        summary.put("totalCommission", totalCommission);
        summary.put("pendingCommission", pendingCommission);
        summary.put("settledCommission", settledCommission);
        summary.put("refundedCommission", refundedCommission);
        return summary;
    }

    @Override
    public List<Map<String, Object>> getCommissionTrend(Long creatorId, String period) {
        LocalDate now = LocalDate.now();
        String startDate;
        String endDate;

        switch (period.toLowerCase()) {
            case "week":
                startDate = now.minusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                endDate = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return commissionMapper.sumDailyCommission(creatorId, startDate, endDate);
            case "month":
                startDate = now.minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                endDate = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return commissionMapper.sumDailyCommission(creatorId, startDate, endDate);
            case "year":
                startDate = now.minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                endDate = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return commissionMapper.sumMonthlyCommission(creatorId, startDate, endDate);
            default:
                startDate = now.minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE);
                endDate = now.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
                return commissionMapper.sumDailyCommission(creatorId, startDate, endDate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void requestWithdraw(Long creatorId, BigDecimal amount) {
        if (amount == null || amount.doubleValue() <= 0) {
            throw new BusinessException("提现金额必须大于0");
        }
        BigDecimal settledCommission = commissionMapper.sumCommissionByCreatorAndStatus(creatorId, "SETTLED");
        if (settledCommission.compareTo(amount) < 0) {
            throw new BusinessException("可提现金额不足");
        }
        log.info("提现申请: creatorId={}, amount={}", creatorId, amount);
    }
}
