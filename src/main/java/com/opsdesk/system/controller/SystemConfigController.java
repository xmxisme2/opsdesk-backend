package com.opsdesk.system.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.system.dto.SystemConfigSearchRequest;
import com.opsdesk.system.service.SystemConfigService;
import com.opsdesk.system.vo.SystemConfigVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统通用配置 Controller，仅向管理员返回经过脱敏的配置列表。 */
@RestController
@RequestMapping("/api/system/configs")
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {
    private final SystemConfigService service;

    public SystemConfigController(SystemConfigService service) {
        this.service = service;
    }

    @PostMapping("/search")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS, keyType = RateLimitKeyType.USER)
    public ApiResponse<List<SystemConfigVO>> search(@RequestBody(required = false) SystemConfigSearchRequest request) {
        return ApiResponse.success(service.search(request));
    }
}
