package com.opsdesk.department.controller;

import com.opsdesk.common.idempotency.Idempotent;
import com.opsdesk.common.ratelimit.RateLimit;
import com.opsdesk.common.ratelimit.RateLimitDefaults;
import com.opsdesk.common.ratelimit.RateLimitKeyType;
import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.department.dto.DepartmentCreateRequest;
import com.opsdesk.department.dto.DepartmentTreeRequest;
import com.opsdesk.department.dto.DepartmentUpdateRequest;
import com.opsdesk.department.service.DepartmentService;
import com.opsdesk.department.vo.DepartmentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理 Controller。
 *
 * <p>只负责接收部门树和后台部门维护请求，权限入口、限流和幂等在控制层声明，业务规则下沉到服务层。</p>
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping("/tree")
    @PreAuthorize("isAuthenticated()")
    @RateLimit(limit = RateLimitDefaults.SEARCH_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<List<DepartmentVO>> tree(@RequestBody(required = false) DepartmentTreeRequest request) {
        return ApiResponse.success(departmentService.tree(request));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DepartmentVO> create(@Valid @RequestBody DepartmentCreateRequest request,
                                            @AuthenticationPrincipal CurrentUser currentUser,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(departmentService.create(
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/detail")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentVO> detail(@PathVariable String id) {
        return ApiResponse.success(departmentService.detail(id));
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<DepartmentVO> update(@PathVariable String id,
                                            @Valid @RequestBody DepartmentUpdateRequest request,
                                            @AuthenticationPrincipal CurrentUser currentUser,
                                            HttpServletRequest servletRequest) {
        return ApiResponse.success(departmentService.update(
                id,
                request,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @Idempotent
    @RateLimit(limit = RateLimitDefaults.ACTION_LIMIT_PER_MINUTE,
            windowSeconds = RateLimitDefaults.ONE_MINUTE_SECONDS,
            keyType = RateLimitKeyType.USER)
    public ApiResponse<Void> delete(@PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser currentUser,
                                    HttpServletRequest servletRequest) {
        departmentService.delete(
                id,
                currentUser.getUserId(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success();
    }
}
