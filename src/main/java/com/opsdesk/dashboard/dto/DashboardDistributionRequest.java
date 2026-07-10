package com.opsdesk.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板分布请求。
 *
 * <p>dimension 只允许 category、priority、status，避免外部直接拼接任意统计字段。</p>
 */
@Getter
@Setter
public class DashboardDistributionRequest extends DashboardRangeRequest {

    private String dimension;
}
