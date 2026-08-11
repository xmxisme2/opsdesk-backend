package com.opsdesk.system.controller;

import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.system.service.AiSettingsService;
import com.opsdesk.system.vo.AiSettingsVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 设置查询 Controller，仅允许管理员查看公开运行状态，绝不返回模型密钥。 */
@RestController
@RequestMapping("/api/system/ai-settings")
@PreAuthorize("hasRole('ADMIN')")
public class AiSettingsController {
    private final AiSettingsService service;

    public AiSettingsController(AiSettingsService service) {
        this.service = service;
    }

    @PostMapping("/detail")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS, keyType = RateLimitKeyType.USER)
    public ApiResponse<AiSettingsVO> detail(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(service.detail(currentUser));
    }
}
