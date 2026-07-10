package com.opsdesk.dashboard.vo;

/**
 * 数据看板总览指标响应。
 */
public record DashboardSummaryVO(
        long todayCreated,
        long pendingCount,
        long processingCount,
        long overdueCount,
        double avgProcessDuration,
        double completionRate
) {
}
