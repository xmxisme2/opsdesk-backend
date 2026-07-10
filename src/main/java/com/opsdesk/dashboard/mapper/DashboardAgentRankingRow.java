package com.opsdesk.dashboard.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板处理人排行 SQL 行。
 */
@Getter
@Setter
public class DashboardAgentRankingRow {
    private Long userId;
    private String userName;
    private Long completedCount;
    private Double avgProcessDuration;
    private Long overdueCount;
}
