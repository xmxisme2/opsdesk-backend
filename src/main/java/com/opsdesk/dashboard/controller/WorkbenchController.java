package com.opsdesk.dashboard.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.dashboard.dto.WorkbenchSummaryRequest;
import com.opsdesk.dashboard.service.WorkbenchService;
import com.opsdesk.dashboard.vo.WorkbenchSummaryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台 Controller。
 *
 * <p>只负责登录权限、列表级限流和调用服务层；摘要数据范围由服务层按当前用户收敛。</p>
 */
@RestController
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    public WorkbenchController(WorkbenchService workbenchService) {
        this.workbenchService = workbenchService;
    }

    @PostMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<WorkbenchSummaryVO> summary(@RequestBody(required = false) WorkbenchSummaryRequest request,
                                                   @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(workbenchService.summary(request == null ? null : request.getTeamId(), currentUser));
    }
}
