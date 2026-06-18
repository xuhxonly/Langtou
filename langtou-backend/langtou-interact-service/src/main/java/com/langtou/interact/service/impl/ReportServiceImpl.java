package com.langtou.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.common.exception.BusinessException;
import com.langtou.common.result.ResultCode;
import com.langtou.interact.dto.ReportCreateDTO;
import com.langtou.interact.entity.Report;
import com.langtou.interact.mapper.ReportMapper;
import com.langtou.interact.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    public Report createReport(Long reporterId, Long noteId, ReportCreateDTO dto) {
        Report report = new Report();
        report.setReporterId(reporterId);
        report.setNoteId(noteId);
        report.setReason(dto.getReason());
        report.setReportType(dto.getReportType());
        report.setStatus(0);
        reportMapper.insert(report);
        log.info("笔记举报成功: reporterId={}, noteId={}, reportType={}", reporterId, noteId, dto.getReportType());
        return report;
    }

    @Override
    public Page<Report> getReportList(int page, int size, Integer status) {
        Page<Report> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreatedAt);
        return reportMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public Report getReportDetail(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        return report;
    }

    @Override
    public void handleReport(Long id, String handleResult, String action) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报不存在");
        }
        if (report.getStatus() == 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }
        report.setStatus(1);
        reportMapper.updateById(report);
        log.info("举报处理完成: reportId={}, action={}, handleResult={}", id, action, handleResult);
    }
}
