package com.opsdesk.notification.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.notification.dto.NotificationReadAllRequest;
import com.opsdesk.notification.dto.NotificationSearchRequest;
import com.opsdesk.notification.service.NotificationService;
import com.opsdesk.notification.vo.NotificationReadAllVO;
import com.opsdesk.notification.vo.NotificationUnreadCountVO;
import com.opsdesk.notification.vo.NotificationVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知中心 Controller。
 *
 * <p>只负责登录权限、限流和调用 Service；通知接收人范围由 Service 按当前用户强制收敛。</p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<NotificationVO>> search(@RequestBody(required = false) NotificationSearchRequest request,
                                                          @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.search(request, currentUser));
    }

    @PostMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<NotificationUnreadCountVO> unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.unreadCount(currentUser));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<NotificationVO> markRead(@PathVariable String id,
                                                @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.markRead(id, currentUser));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<NotificationReadAllVO> readAll(@RequestBody(required = false) NotificationReadAllRequest request,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(notificationService.readAll(request, currentUser));
    }
}
