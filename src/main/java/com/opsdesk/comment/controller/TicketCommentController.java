package com.opsdesk.comment.controller;

import com.opsdesk.comment.dto.CommentCreateRequest;
import com.opsdesk.comment.dto.CommentSearchRequest;
import com.opsdesk.comment.service.TicketCommentService;
import com.opsdesk.comment.vo.CommentVO;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工单评论 Controller。
 *
 * <p>只负责参数接收、鉴权入口、限流幂等和调用 Service；评论资源范围和内部备注权限由 Service 统一判断。</p>
 */
@RestController
@RequestMapping("/api")
public class TicketCommentController {

    private final TicketCommentService commentService;

    public TicketCommentController(TicketCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/tickets/{ticketId}/comments/create")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<CommentVO> create(@PathVariable String ticketId,
                                         @RequestBody CommentCreateRequest request,
                                         @AuthenticationPrincipal CurrentUser currentUser,
                                         HttpServletRequest servletRequest) {
        return ApiResponse.success(commentService.create(ticketId, request, currentUser,
                requestIp(servletRequest), userAgent(servletRequest)));
    }

    @PostMapping("/tickets/{ticketId}/comments/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<CommentVO>> search(@PathVariable String ticketId,
                                                     @RequestBody(required = false) CommentSearchRequest request,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(commentService.search(ticketId, request, currentUser));
    }

    @PostMapping("/comments/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        commentService.delete(id, currentUser, requestIp(servletRequest), userAgent(servletRequest));
        return ApiResponse.success();
    }

    private String requestIp(HttpServletRequest servletRequest) {
        return servletRequest.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest servletRequest) {
        return servletRequest.getHeader("User-Agent");
    }
}
