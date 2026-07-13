package com.opsdesk.audit.controller;

import com.opsdesk.audit.dto.AuditLogSearchRequest;
import com.opsdesk.audit.service.AuditLogService;
import com.opsdesk.audit.vo.AuditLogVO;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统审计日志 Controller。
 *
 * <p>只允许管理员分页检索，不提供删除或修改入口。</p>
 */
@RestController
@RequestMapping("/api/audit/logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<AuditLogVO>> search(@RequestBody(required = false) AuditLogSearchRequest request) {
        return ApiResponse.success(auditLogService.search(request));
    }
}
