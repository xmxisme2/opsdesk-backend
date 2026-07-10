package com.opsdesk.dashboard.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板趋势 SQL 行。
 */
@Getter
@Setter
public class DashboardTrendRow {
    private String statDate;
    private Long createdCount;
    private Long completedCount;
    private Long overdueCount;
}
