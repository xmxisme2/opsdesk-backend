package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.EmailNotificationSettingsUpdateRequest;
import com.opsdesk.system.service.EmailNotificationSettingsService;
import com.opsdesk.system.vo.EmailNotificationSettingsVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 邮件通知配置接口，仅允许管理员维护默认收件邮箱和发送开关。 */
@RestController
@RequestMapping("/api/system/email-notification-settings")
@PreAuthorize("hasRole('ADMIN')")
public class EmailNotificationSettingsController {
    private final EmailNotificationSettingsService service;

    public EmailNotificationSettingsController(EmailNotificationSettingsService service) {
        this.service = service;
    }

    @PostMapping("/detail")
    public ApiResponse<EmailNotificationSettingsVO> detail() {
        return ApiResponse.success(service.detail());
    }

    @PostMapping("/update")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<EmailNotificationSettingsVO> update(@Valid @RequestBody EmailNotificationSettingsUpdateRequest request,
                                                            @AuthenticationPrincipal CurrentUser user,
                                                            HttpServletRequest servlet) {
        return ApiResponse.success(service.update(request, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent")));
    }
}
