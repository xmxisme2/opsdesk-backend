package com.opsdesk.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板处理人排行请求。
 *
 * <p>limit 控制排行数量，服务层会限制最大值，避免一次性返回过多处理人。</p>
 */
@Getter
@Setter
public class DashboardAgentRankingRequest extends DashboardRangeRequest {

    private Integer limit;
}
