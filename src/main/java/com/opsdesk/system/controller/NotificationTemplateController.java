package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.*;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.NotificationTemplateSearchRequest;
import com.opsdesk.system.dto.NotificationTemplateUpdateRequest;
import com.opsdesk.system.service.NotificationTemplateService;
import com.opsdesk.system.vo.NotificationTemplateVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 通知模板管理 Controller，仅开放给 ADMIN 并负责请求转发。 */
@RestController @RequestMapping("/api/system/notification-templates") @PreAuthorize("hasRole('ADMIN')")
public class NotificationTemplateController {
    private final NotificationTemplateService service;
    public NotificationTemplateController(NotificationTemplateService service) { this.service = service; }
    @PostMapping("/search")
    @RateLimit(limit=RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<List<NotificationTemplateVO>> search(@RequestBody(required=false) NotificationTemplateSearchRequest request) { return ApiResponse.success(service.search(request)); }
    @PostMapping("/{id}/update") @Idempotent
    @RateLimit(limit=RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<NotificationTemplateVO> update(@PathVariable String id, @Valid @RequestBody NotificationTemplateUpdateRequest request, @AuthenticationPrincipal CurrentUser user, HttpServletRequest servlet) {
        return ApiResponse.success(service.update(id, request, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent")));
    }
}
