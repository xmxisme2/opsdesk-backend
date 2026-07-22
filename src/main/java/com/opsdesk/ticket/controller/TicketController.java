package com.opsdesk.ticket.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.pagination.PageQuery;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.ticket.dto.TicketAssignRequest;
import com.opsdesk.ticket.dto.TicketCompleteRequest;
import com.opsdesk.ticket.dto.TicketCreateRequest;
import com.opsdesk.ticket.dto.TicketReasonRequest;
import com.opsdesk.ticket.dto.TicketSearchRequest;
import com.opsdesk.ticket.dto.TicketTransferRequest;
import com.opsdesk.ticket.dto.TicketUpdateRequest;
import com.opsdesk.ticket.service.TicketService;
import com.opsdesk.ticket.vo.TicketListItemVO;
import com.opsdesk.ticket.vo.TicketOperationLogVO;
import com.opsdesk.ticket.vo.TicketVO;
import com.opsdesk.ticket.vo.TicketWatchVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 工单主流程 Controller。
 *
 * <p>只负责接收参数、权限入口、限流幂等和调用 Service；工单状态必须由 TicketService 通过状态机流转。</p>
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> create(@Valid @RequestBody TicketCreateRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.create(request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> updateDraft(@PathVariable String id,
                                             @Valid @RequestBody TicketUpdateRequest request,
                                             @AuthenticationPrincipal CurrentUser currentUser,
                                             HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.updateDraft(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<TicketListItemVO>> search(@RequestBody(required = false) TicketSearchRequest request,
                                                            @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(ticketService.search(request, currentUser));
    }

    /**
     * 导出当前筛选范围内的工单，文件流不使用统一 JSON 包装，避免前端无法直接保存工作簿。
     */
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public void export(@RequestBody(required = false) TicketSearchRequest request,
                       @AuthenticationPrincipal CurrentUser currentUser,
                       HttpServletRequest servletRequest,
                       HttpServletResponse response) throws IOException {
        byte[] content = ticketService.export(request, currentUser, requestIp(servletRequest), userAgent(servletRequest));
        String filename = URLEncoder.encode("工单导出.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    @PostMapping("/{id}/detail")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TicketVO> detail(@PathVariable String id,
                                        @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(ticketService.detail(id, currentUser));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> submit(@PathVariable String id,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.submit(id, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> assign(@PathVariable String id,
                                        @RequestBody(required = false) TicketAssignRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.assign(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> reject(@PathVariable String id,
                                        @RequestBody(required = false) TicketReasonRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.reject(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> accept(@PathVariable String id,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.accept(id, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/transfer")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> transfer(@PathVariable String id,
                                          @Valid @RequestBody TicketTransferRequest request,
                                          @AuthenticationPrincipal CurrentUser currentUser,
                                          HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.transfer(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> complete(@PathVariable String id,
                                          @RequestBody(required = false) TicketCompleteRequest request,
                                          @AuthenticationPrincipal CurrentUser currentUser,
                                          HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.complete(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> confirm(@PathVariable String id,
                                         @RequestBody(required = false) TicketReasonRequest request,
                                         @AuthenticationPrincipal CurrentUser currentUser,
                                         HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.confirm(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> reopen(@PathVariable String id,
                                        @RequestBody(required = false) TicketReasonRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.reopen(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> close(@PathVariable String id,
                                       @RequestBody(required = false) TicketReasonRequest request,
                                       @AuthenticationPrincipal CurrentUser currentUser,
                                       HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.close(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<TicketVO> cancel(@PathVariable String id,
                                        @RequestBody(required = false) TicketReasonRequest request,
                                        @AuthenticationPrincipal CurrentUser currentUser,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.cancel(id, request, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/watch")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    public ApiResponse<TicketWatchVO> watch(@PathVariable String id,
                                            @AuthenticationPrincipal CurrentUser currentUser,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.watch(id, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/unwatch")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    public ApiResponse<TicketWatchVO> unwatch(@PathVariable String id,
                                              @AuthenticationPrincipal CurrentUser currentUser,
                                              HttpServletRequest servletRequest) {
        return ApiResponse.success(ticketService.unwatch(id, currentUser, requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/{id}/operation-logs/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<TicketOperationLogVO>> searchOperationLogs(@PathVariable String id,
                                                                             @RequestBody(required = false) PageQuery request,
                                                                             @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(ticketService.searchOperationLogs(id, request, currentUser));
    }

    private String requestIp(HttpServletRequest servletRequest) {
        return servletRequest.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest servletRequest) {
        return servletRequest.getHeader("User-Agent");
    }
}
