package com.opsdesk.dashboard.vo;

import java.util.List;

/**
 * 数据看板处理人排行响应。
 */
public record DashboardAgentRankingVO(
        List<DashboardAgentRankingItemVO> items
) {
}
