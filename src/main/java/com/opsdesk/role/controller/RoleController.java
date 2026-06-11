package com.opsdesk.role.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.response.PageResult;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.role.dto.RoleCreateRequest;
import com.opsdesk.role.dto.RolePermissionUpdateRequest;
import com.opsdesk.role.dto.RoleSearchRequest;
import com.opsdesk.role.dto.RoleUpdateRequest;
import com.opsdesk.role.service.RoleService;
import com.opsdesk.role.vo.RoleVO;
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
 * 角色管理 Controller。
 *
 * <p>只负责接收角色管理页请求、执行 ADMIN 权限入口校验并返回统一响应，具体业务规则下沉到 RoleService。</p>
 */
@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/search")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<PageResult<RoleVO>> search(@RequestBody(required = false) RoleSearchRequest request) {
        return ApiResponse.success(roleService.search(request));
    }

    @PostMapping("/create")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<RoleVO> create(@Valid @RequestBody RoleCreateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(roleService.create(
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/detail")
    public ApiResponse<RoleVO> detail(@PathVariable String id) {
        return ApiResponse.success(roleService.detail(id));
    }

    @PostMapping("/{id}/update")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<RoleVO> update(@PathVariable String id,
                                      @Valid @RequestBody RoleUpdateRequest request,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      HttpServletRequest servletRequest) {
        return ApiResponse.success(roleService.update(
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
        roleService.delete(
                id,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success();
    }

    @PostMapping("/{id}/permissions/update")
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<RoleVO> updatePermissions(@PathVariable String id,
                                                 @Valid @RequestBody RolePermissionUpdateRequest request,
                                                 @AuthenticationPrincipal CurrentUser currentUser,
                                                 HttpServletRequest servletRequest) {
        return ApiResponse.success(roleService.updatePermissions(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }
}
