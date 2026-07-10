package com.opsdesk.dashboard.dto;

import com.opsdesk.common.pagination.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据看板超时工单分页请求。
 *
 * <p>复用公共分页模型，仅补充团队和优先级筛选条件。</p>
 */
@Getter
@Setter
public class DashboardOverdueTicketsRequest extends PageQuery {

    private String teamId;

    private String priority;
}
