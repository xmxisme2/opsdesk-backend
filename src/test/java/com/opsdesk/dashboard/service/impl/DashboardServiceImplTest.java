package com.opsdesk.dashboard.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.dto.DashboardDistributionRequest;
import com.opsdesk.dashboard.dto.DashboardRangeRequest;
import com.opsdesk.dashboard.mapper.DashboardAgentRankingRow;
import com.opsdesk.dashboard.mapper.DashboardDistributionRow;
import com.opsdesk.dashboard.vo.DashboardAgentRankingVO;
import com.opsdesk.dashboard.vo.DashboardDistributionVO;
import com.opsdesk.dashboard.vo.DashboardSummaryVO;
import com.opsdesk.team.mapper.TeamMapper;
import com.opsdesk.ticket.converter.TicketConverter;
import com.opsdesk.ticket.mapper.TicketCategoryMapper;
import com.opsdesk.ticket.mapper.TicketMapper;
import com.opsdesk.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 数据看板服务测试。
 *
 * <p>覆盖统计口径组装、维度白名单和登录态校验，避免看板接口绕过服务层规则。</p>
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper ticketCategoryMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private TeamMapper teamMapper;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                ticketMapper,
                ticketCategoryMapper,
                sysUserMapper,
                teamMapper,
                new TicketConverter()
        );
    }

    @Test
    void summaryShouldAggregateDashboardMetrics() {
        CurrentUser currentUser = user(10L, "ADMIN");
        DashboardRangeRequest request = new DashboardRangeRequest();
        request.setDateFrom("2026-07-01");
        request.setDateTo("2026-07-10");
        when(ticketMapper.countDashboardCreated(any(), any(), eq(null), eq(10L), eq(true))).thenReturn(6L);
        when(ticketMapper.countDashboardByStatusGroup(anyList(), any(), any(), eq(null), eq(10L), eq(true)))
                .thenReturn(8L, 4L, 9L);
        when(ticketMapper.countDashboardOverdue(any(), any(), eq(null), eq(10L), eq(true))).thenReturn(2L);
        when(ticketMapper.avgDashboardProcessHours(any(), any(), eq(null), eq(10L), eq(true))).thenReturn(3.76D);
        when(ticketMapper.countDashboardTotal(any(), any(), eq(null), eq(10L), eq(true))).thenReturn(18L);

        DashboardSummaryVO summary = dashboardService.summary(request, currentUser);

        assertThat(summary.todayCreated()).isEqualTo(6);
        assertThat(summary.pendingCount()).isEqualTo(8);
        assertThat(summary.processingCount()).isEqualTo(4);
        assertThat(summary.overdueCount()).isEqualTo(2);
        assertThat(summary.avgProcessDuration()).isEqualTo(3.8D);
        assertThat(summary.completionRate()).isEqualTo(50.0D);
    }

    @Test
    void distributionsShouldRejectUnknownDimension() {
        DashboardDistributionRequest request = new DashboardDistributionRequest();
        request.setDimension("assignee");

        assertThatThrownBy(() -> dashboardService.distributions(request, user(10L, "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARAM_ERROR);
    }

    @Test
    void distributionsShouldReturnPriorityItems() {
        DashboardDistributionRequest request = new DashboardDistributionRequest();
        request.setDimension("priority");
        DashboardDistributionRow row = new DashboardDistributionRow();
        row.setName("URGENT");
        row.setValue(3L);
        when(ticketMapper.findDashboardDistribution(eq("priority"), any(), any(), eq(null), eq(10L), eq(true)))
                .thenReturn(List.of(row));

        DashboardDistributionVO distribution = dashboardService.distributions(request, user(10L, "ADMIN"));

        assertThat(distribution.dimension()).isEqualTo("priority");
        assertThat(distribution.items()).extracting("name").containsExactly("URGENT");
    }

    @Test
    void agentRankingShouldLimitAndRoundResult() {
        DashboardAgentRankingRow row = new DashboardAgentRankingRow();
        row.setUserId(20L);
        row.setUserName("赵晨");
        row.setCompletedCount(12L);
        row.setAvgProcessDuration(2.94D);
        row.setOverdueCount(1L);
        when(ticketMapper.findDashboardAgentRanking(any(), any(), eq(null), eq(10L), anyBoolean(), eq(5)))
                .thenReturn(List.of(row));

        DashboardAgentRankingVO ranking = dashboardService.agentRanking(null, user(10L, "MANAGER"));

        assertThat(ranking.items()).hasSize(1);
        assertThat(ranking.items().get(0).userId()).isEqualTo("20");
        assertThat(ranking.items().get(0).avgProcessDuration()).isEqualTo(2.9D);
    }

    @Test
    void summaryShouldRejectAnonymousUser() {
        assertThatThrownBy(() -> dashboardService.summary(null, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private CurrentUser user(Long userId, String role) {
        return new CurrentUser(userId, "13800000000", "user" + userId, List.of(role), List.of());
    }
}
