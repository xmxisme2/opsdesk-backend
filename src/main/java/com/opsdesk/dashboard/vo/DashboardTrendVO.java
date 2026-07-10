package com.opsdesk.dashboard.vo;

import java.util.List;

/**
 * 数据看板趋势响应。
 */
public record DashboardTrendVO(
        List<DashboardTrendPointVO> points
) {
}
