package com.opsdesk.ticket.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.ticket.dto.TicketCategoryTreeRequest;
import com.opsdesk.ticket.service.TicketCategoryService;
import com.opsdesk.ticket.vo.TicketCategoryVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单分类 Controller。
 *
 * <p>提供创建工单和工单列表筛选需要的分类树查询，分类维护后续由系统配置模块承接。</p>
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
}
