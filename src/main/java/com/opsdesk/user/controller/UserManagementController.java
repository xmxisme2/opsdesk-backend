package com.opsdesk.user.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.user.dto.UserCreateRequest;
import com.opsdesk.user.dto.UserResetPasswordRequest;
import com.opsdesk.user.dto.UserSearchRequest;
import com.opsdesk.user.dto.UserStatusUpdateRequest;
import com.opsdesk.user.dto.UserUpdateRequest;
import com.opsdesk.user.service.UserManagementService;
import com.opsdesk.user.vo.UserResetPasswordVO;
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
 * 后台用户管理 Controller。
 *
 * <p>只负责接收用户管理页 CRUD 请求、执行 ADMIN 权限入口校验和返回统一响应，具体业务规则下沉到 UserManagementService。</p>
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @PostMapping("/search")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<UserVO>> search(@RequestBody(required = false) UserSearchRequest request) {
        return ApiResponse.success(userManagementService.search(request));
    }

    @PostMapping("/create")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(userManagementService.create(
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/detail")
    public ApiResponse<UserVO> detail(@PathVariable String id) {
        return ApiResponse.success(userManagementService.detail(id));
    }

    @PostMapping("/{id}/update")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<UserVO> update(@PathVariable String id,
                                      @Valid @RequestBody UserUpdateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(userManagementService.update(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/status")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<UserVO> updateStatus(@PathVariable String id,
                                            @Valid @RequestBody UserStatusUpdateRequest request,
                                            @AuthenticationPrincipal CurrentUser currentUser,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(userManagementService.updateStatus(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/delete")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        userManagementService.delete(
                id,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success();
    }

    @PostMapping("/{id}/reset-password")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<UserResetPasswordVO> resetPassword(@PathVariable String id,
                                                         @Valid @RequestBody(required = false) UserResetPasswordRequest request,
                                                         @AuthenticationPrincipal CurrentUser currentUser,
                                                         HttpServletRequest servletRequest) {
        return ApiResponse.success(userManagementService.resetPassword(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }
}
