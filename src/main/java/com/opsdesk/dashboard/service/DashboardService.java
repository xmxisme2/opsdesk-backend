package com.opsdesk.dashboard.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.dto.DashboardAgentRankingRequest;
import com.opsdesk.dashboard.dto.DashboardDistributionRequest;
import com.opsdesk.dashboard.dto.DashboardOverdueTicketsRequest;
import com.opsdesk.dashboard.dto.DashboardRangeRequest;
import com.opsdesk.dashboard.dto.DashboardTrendRequest;
import com.opsdesk.dashboard.vo.DashboardAgentRankingVO;
import com.opsdesk.dashboard.vo.DashboardDistributionVO;
import com.opsdesk.dashboard.vo.DashboardSummaryVO;
import com.opsdesk.dashboard.vo.DashboardTrendVO;
import com.opsdesk.ticket.vo.TicketListItemVO;

/**
 * 数据看板服务。
 *
 * <p>统一处理看板统计口径、角色数据范围和工单列表分页。</p>
 */
public interface DashboardService {

    DashboardSummaryVO summary(DashboardRangeRequest request, CurrentUser currentUser);

    DashboardTrendVO trends(DashboardTrendRequest request, CurrentUser currentUser);

    DashboardDistributionVO distributions(DashboardDistributionRequest request, CurrentUser currentUser);

    DashboardAgentRankingVO agentRanking(DashboardAgentRankingRequest request, CurrentUser currentUser);

    PageResult<TicketListItemVO> overdueTickets(DashboardOverdueTicketsRequest request, CurrentUser currentUser);
}
