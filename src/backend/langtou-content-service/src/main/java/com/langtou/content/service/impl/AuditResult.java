package com.langtou.content.service.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 内容审核结果
 * 支持通过(PASS)、拒绝(REJECT)、人工复核(REVIEW)三种状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResult {

    /**
     * 审核状态：PASS / REJECT / REVIEW
     */
    private String status;

    /**
     * 审核不通过或需复核的原因
     */
    private String reason;

    /**
     * 命中的风险标签列表（如：porn, violence, politics, ad 等）
     */
    private List<String> riskLabels;

    /**
     * 置信度分数（0.0 ~ 1.0）
     */
    private Double confidence;

    /**
     * 审核服务提供商（aliyun / tencent / baidu / local）
     */
    private String provider;

    /**
     * 审核耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 原始响应（调试用）
     */
    private String rawResponse;

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_REJECT = "REJECT";
    public static final String STATUS_REVIEW = "REVIEW";

    public boolean isPass() {
        return STATUS_PASS.equalsIgnoreCase(status);
    }

    public boolean isReject() {
        return STATUS_REJECT.equalsIgnoreCase(status);
    }

    public boolean isReview() {
        return STATUS_REVIEW.equalsIgnoreCase(status);
    }

    public static AuditResult pass(String provider) {
        return AuditResult.builder()
                .status(STATUS_PASS)
                .provider(provider)
                .build();
    }

    public static AuditResult reject(String reason, String provider) {
        return AuditResult.builder()
                .status(STATUS_REJECT)
                .reason(reason)
                .provider(provider)
                .build();
    }

    public static AuditResult review(String reason, List<String> riskLabels, String provider) {
        return AuditResult.builder()
                .status(STATUS_REVIEW)
                .reason(reason)
                .riskLabels(riskLabels)
                .provider(provider)
                .build();
    }
}
