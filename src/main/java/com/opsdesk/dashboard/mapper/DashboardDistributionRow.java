package com.opsdesk.dashboard.mapper;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板分布 SQL 行。
 */
@Getter
@Setter
public class DashboardDistributionRow {
    private String name;
    private Long value;
}
