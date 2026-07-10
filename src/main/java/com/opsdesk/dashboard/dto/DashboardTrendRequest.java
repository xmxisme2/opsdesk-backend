package com.opsdesk.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板趋势请求。
 *
 * <p>range 支持 7d 和 30d，未传日期范围时由服务层按 range 计算默认窗口。</p>
 */
@Getter
@Setter
public class DashboardTrendRequest extends DashboardRangeRequest {

    private String range;
}
