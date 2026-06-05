package com.opsdesk.role.service;

import com.opsdesk.common.response.PageResult;
import com.opsdesk.role.dto.RoleCreateRequest;
import com.opsdesk.role.dto.RolePermissionUpdateRequest;
import com.opsdesk.role.dto.RoleSearchRequest;
import com.opsdesk.role.dto.RoleUpdateRequest;
import com.opsdesk.role.vo.RoleVO;

/**
 * 角色服务。
 *
 * <p>负责角色增删改查、权限分配、内置角色保护和权限缓存失效。</p>
 */
public interface RoleService {

    PageResult<RoleVO> search(RoleSearchRequest request);

    RoleVO create(RoleCreateRequest request, Long operatorId, String requestIp, String userAgent);

    RoleVO detail(String id);

    RoleVO update(String id, RoleUpdateRequest request, Long operatorId, String requestIp, String userAgent);

    void delete(String id, Long operatorId, String requestIp, String userAgent);

    RoleVO updatePermissions(String id,
                             RolePermissionUpdateRequest request,
                             Long operatorId,
                             String requestIp,
                             String userAgent);
}
