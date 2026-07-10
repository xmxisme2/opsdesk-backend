package com.opsdesk.dashboard.vo;

/**
 * 数据看板分布项响应。
 */
public record DashboardDistributionItemVO(
        String name,
        long value
) {
}
