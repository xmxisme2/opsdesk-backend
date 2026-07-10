package com.opsdesk.dashboard.vo;

import java.util.List;

/**
 * 数据看板分布响应。
 */
public record DashboardDistributionVO(
        String dimension,
        List<DashboardDistributionItemVO> items
) {
}
