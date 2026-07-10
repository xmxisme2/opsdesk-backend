package com.opsdesk.dashboard.vo;

/**
 * 数据看板趋势点响应。
 */
public record DashboardTrendPointVO(
        String date,
        long createdCount,
        long completedCount,
        long overdueCount
) {
}
