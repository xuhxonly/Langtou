package com.langtou.interact.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.langtou.interact.dto.ReportCreateDTO;
import com.langtou.interact.entity.Report;

import java.util.Map;

public interface ReportService {

    Report createReport(Long reporterId, Long noteId, ReportCreateDTO dto);

    /**
     * 分页查询举报列表
     */
    Page<Report> getReportList(int page, int size, Integer status);

    /**
     * 获取举报详情
     */
    Report getReportDetail(Long id);

    /**
     * 处理举报
     */
    void handleReport(Long id, String handleResult, String action);
}
