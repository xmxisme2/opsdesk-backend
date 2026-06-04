package com.opsdesk.user.service.impl;

import com.opsdesk.common.exception.BusinessException;
import com.opsdesk.common.exception.ErrorCode;
import com.opsdesk.common.security.CurrentUser;
import com.opsdesk.department.entity.Department;
import com.opsdesk.department.mapper.DepartmentMapper;
import com.opsdesk.permission.entity.Permission;
import com.opsdesk.permission.mapper.PermissionMapper;
import com.opsdesk.role.entity.Role;
import com.opsdesk.role.mapper.RoleMapper;
import com.opsdesk.user.converter.UserConverter;
import com.opsdesk.user.entity.SysUser;
import com.opsdesk.user.mapper.SysUserMapper;
import com.opsdesk.user.service.UserContextService;
import com.opsdesk.user.vo.UserVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户上下文服务实现。
 *
 * <p>每次加载时检查账号状态，禁用或锁定账号不能继续访问业务接口。</p>
 */
@Service
public class UserContextServiceImpl implements UserContextService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final DepartmentMapper departmentMapper;
    private final UserConverter userConverter;

    public UserContextServiceImpl(SysUserMapper sysUserMapper,
                                  RoleMapper roleMapper,
                                  PermissionMapper permissionMapper,
                                  DepartmentMapper departmentMapper,
                                  UserConverter userConverter) {
        this.sysUserMapper = sysUserMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.departmentMapper = departmentMapper;
        this.userConverter = userConverter;
    }

    @Override
    public CurrentUser loadCurrentUser(Long userId) {
        SysUser user = loadActiveUser(userId);
        List<Role> roles = roleMapper.findEnabledByUserId(userId);
        List<Permission> permissions = permissionMapper.findEnabledByUserId(userId);
        return new CurrentUser(
                user.getId(),
                user.getPhone(),
                user.getUsername(),
                roles.stream().map(Role::getCode).toList(),
                permissions.stream().map(Permission::getCode).toList()
        );
    }

    @Override
    public UserVO loadUserVO(Long userId) {
        SysUser user = loadActiveUser(userId);
        Department department = user.getDepartmentId() == null ? null : departmentMapper.findEnabledById(user.getDepartmentId());
        List<Role> roles = roleMapper.findEnabledByUserId(userId);
        List<Permission> permissions = permissionMapper.findEnabledByUserId(userId);
        return userConverter.toVO(user, department, roles, permissions);
    }

    private SysUser loadActiveUser(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在");
        }
        if (!STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用或锁定");
        }
        return user;
    }
}
