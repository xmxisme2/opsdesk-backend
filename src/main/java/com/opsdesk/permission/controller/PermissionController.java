package com.opsdesk.permission.controller;

import com.opsdesk.common.response.ApiResponse;
import com.opsdesk.permission.dto.PermissionTreeRequest;
import com.opsdesk.permission.service.PermissionService;
import com.opsdesk.permission.vo.PermissionVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理 Controller。
 *
 * <p>为角色授权页提供权限树查询入口，当前仅 ADMIN 可读取完整权限配置。</p>
 */
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/tree")
    public ApiResponse<List<PermissionVO>> tree(@RequestBody(required = false) PermissionTreeRequest request) {
        return ApiResponse.success(permissionService.tree(request));
    }
}
