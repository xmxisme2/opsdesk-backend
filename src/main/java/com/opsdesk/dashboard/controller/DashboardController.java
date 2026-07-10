package com.opsdesk.dashboard.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.dto.DashboardAgentRankingRequest;
import com.opsdesk.dashboard.dto.DashboardDistributionRequest;
import com.opsdesk.dashboard.dto.DashboardOverdueTicketsRequest;
import com.opsdesk.dashboard.dto.DashboardRangeRequest;
import com.opsdesk.dashboard.dto.DashboardTrendRequest;
import com.opsdesk.dashboard.service.DashboardService;
import com.opsdesk.dashboard.vo.DashboardAgentRankingVO;
import com.opsdesk.dashboard.vo.DashboardDistributionVO;
import com.opsdesk.dashboard.vo.DashboardSummaryVO;
import com.opsdesk.dashboard.vo.DashboardTrendVO;
import com.opsdesk.ticket.vo.TicketListItemVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据看板 Controller。
 *
 * <p>接口仅对 MANAGER 和 ADMIN 开放，真实数据范围继续由服务层按当前用户收敛。</p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/summary")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DashboardSummaryVO> summary(@RequestBody(required = false) DashboardRangeRequest request,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(dashboardService.summary(request, currentUser));
    }

    @PostMapping("/trends")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DashboardTrendVO> trends(@RequestBody(required = false) DashboardTrendRequest request,
                                                @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(dashboardService.trends(request, currentUser));
    }

    @PostMapping("/distributions")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DashboardDistributionVO> distributions(@RequestBody(required = false) DashboardDistributionRequest request,
                                                              @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(dashboardService.distributions(request, currentUser));
    }

    @PostMapping("/agent-ranking")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DashboardAgentRankingVO> agentRanking(@RequestBody(required = false) DashboardAgentRankingRequest request,
                                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(dashboardService.agentRanking(request, currentUser));
    }

    @PostMapping("/overdue-tickets")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<TicketListItemVO>> overdueTickets(@RequestBody(required = false) DashboardOverdueTicketsRequest request,
                                                                    @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(dashboardService.overdueTickets(request, currentUser));
    }
}
