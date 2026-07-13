package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.*;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.UploadPolicyUpdateRequest;
import com.opsdesk.system.service.UploadPolicyService;
import com.opsdesk.system.vo.UploadPolicyVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 上传限制管理 Controller，仅负责权限入口、参数校验与服务转发。 */
@RestController
@RequestMapping("/api/system/upload-policy")
public class UploadPolicyController {
    private final UploadPolicyService service;
    public UploadPolicyController(UploadPolicyService service) { this.service = service; }

    @PostMapping("/detail")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UploadPolicyVO> detail() { return ApiResponse.success(service.detail()); }

    @PostMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS, keyType = RateLimitKeyType.USER)
    public ApiResponse<UploadPolicyVO> update(@Valid @RequestBody UploadPolicyUpdateRequest request,
                                              @AuthenticationPrincipal CurrentUser user, HttpServletRequest servlet) {
        return ApiResponse.success(service.update(request, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent")));
    }
}
