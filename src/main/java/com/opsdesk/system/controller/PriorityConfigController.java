package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.PriorityConfigUpdateRequest;
import com.opsdesk.system.service.PriorityConfigService;
import com.opsdesk.system.vo.PriorityOptionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 工单固定优先级配置 Controller，仅负责鉴权、限流、请求信息提取和服务转发。 */
@RestController
@RequestMapping("/api/system/priorities")
public class PriorityConfigController {
    private final PriorityConfigService service;

    public PriorityConfigController(PriorityConfigService service) {
        this.service = service;
    }

    /** 登录用户读取可用于工单表单和筛选的固定优先级选项。 */
    @PostMapping("/options")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<List<PriorityOptionVO>> options() {
        return ApiResponse.success(service.options());
    }

    /** 管理员整体更新四项优先级配置。 */
    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<List<PriorityOptionVO>> update(@Valid @RequestBody PriorityConfigUpdateRequest request,
                                                       @AuthenticationPrincipal CurrentUser user,
                                                       HttpServletRequest servletRequest) {
        return ApiResponse.success(service.update(request, user.getUserId(), servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }
}
