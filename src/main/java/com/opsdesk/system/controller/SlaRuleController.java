package com.opsdesk.system.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.dto.SlaRuleMutationRequest;
import com.opsdesk.system.dto.SlaRuleSearchRequest;
import com.opsdesk.system.service.SlaRuleService;
import com.opsdesk.system.vo.SlaRuleVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** SLA 规则管理 Controller，仅负责 ADMIN 入口和请求转发。 */
@RestController
@RequestMapping("/api/system/sla-rules")
@PreAuthorize("hasRole('ADMIN')")
public class SlaRuleController {
    private final SlaRuleService service;
    public SlaRuleController(SlaRuleService service) { this.service = service; }

    @PostMapping("/search")
    @RateLimit(limit=RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<List<SlaRuleVO>> search(@RequestBody(required=false) SlaRuleSearchRequest request) { return ApiResponse.success(service.search(request)); }

    @PostMapping("/create") @Idempotent
    @RateLimit(limit=RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<SlaRuleVO> create(@Valid @RequestBody SlaRuleMutationRequest request, @AuthenticationPrincipal CurrentUser user, HttpServletRequest servlet) {
        return ApiResponse.success(service.create(request, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent")));
    }

    @PostMapping("/{id}/update") @Idempotent
    @RateLimit(limit=RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<SlaRuleVO> update(@PathVariable String id, @Valid @RequestBody SlaRuleMutationRequest request, @AuthenticationPrincipal CurrentUser user, HttpServletRequest servlet) {
        return ApiResponse.success(service.update(id, request, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent")));
    }

    @PostMapping("/{id}/delete") @Idempotent
    @RateLimit(limit=RateLimitDefaults.ACTION_LIMIT_PER_MINUTE, windowSeconds=RateLimitDefaults.ONE_MINUTE_SECONDS, keyType=RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, HttpServletRequest servlet) {
        service.delete(id, user.getUserId(), servlet.getRemoteAddr(), servlet.getHeader("User-Agent"));
        return ApiResponse.success();
    }
}
