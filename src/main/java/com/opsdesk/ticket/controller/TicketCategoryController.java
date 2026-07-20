package com.opsdesk.ticket.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketCategoryMutationRequest;
import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.service.TicketCategoryService;
import com.opsdesk.ticket.vo.TicketCategoryVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单分类 Controller。
 *
 * <p>提供分类树查询和管理员写入口；控制层只负责权限、幂等、限流、参数接收和调用服务。</p>
 */
@RestController
@RequestMapping("/api/ticket-categories")
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    public TicketCategoryController(TicketCategoryService ticketCategoryService) {
        this.ticketCategoryService = ticketCategoryService;
    }

    @PostMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<List<TicketCategoryVO>> tree(@RequestBody(required = false) TicketCategoryTreeRequest request) {
        return ApiResponse.success(ticketCategoryService.tree(request));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketCategoryVO> create(@Valid @RequestBody TicketCategoryMutationRequest request,
                                                @AuthenticationPrincipal CurrentUser currentUser,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketCategoryService.create(
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketCategoryVO> update(@PathVariable String id,
                                                @Valid @RequestBody TicketCategoryMutationRequest request,
                                                @AuthenticationPrincipal CurrentUser currentUser,
                                                HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketCategoryService.update(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        ticketCategoryService.delete(
                id,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success();
    }
}
