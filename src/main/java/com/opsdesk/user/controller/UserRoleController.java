package com.opsdesk.user.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.user.dto.UserRoleUpdateRequest;
import com.opsdesk.user.service.UserRoleService;
import com.opsdesk.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户角色绑定 Controller。
 *
 * <p>服务于后台用户管理页的角色分配动作，接口本身只做 ADMIN 权限入口和参数接收。</p>
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserRoleController {

    private final UserRoleService userRoleService;

    public UserRoleController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    @PostMapping("/{id}/roles/update")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<UserVO> updateRoles(@PathVariable String id,
                                           @Valid @RequestBody UserRoleUpdateRequest request,
                                           @AuthenticationPrincipal CurrentUser currentUser,
                                           HttpServletRequest servletRequest) {
        return ApiResponse.success(userRoleService.updateUserRoles(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }
}
