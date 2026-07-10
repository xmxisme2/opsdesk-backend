package com.opsdesk.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板时间范围请求。
 *
 * <p>用于总览、趋势、分布和排行接口，团队 ID 与日期范围均由服务层统一解析和校验。</p>
 */
@Getter
@Setter
public class DashboardRangeRequest {

    private String teamId;

    private String dateFrom;

    private String dateTo;
}
