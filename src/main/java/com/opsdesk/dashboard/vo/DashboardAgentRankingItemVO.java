package com.opsdesk.dashboard.vo;

/**
 * 数据看板处理人排行项响应。
 */
public record DashboardAgentRankingItemVO(
        String userId,
        String userName,
        long completedCount,
        double avgProcessDuration,
        long overdueCount
) {
}
